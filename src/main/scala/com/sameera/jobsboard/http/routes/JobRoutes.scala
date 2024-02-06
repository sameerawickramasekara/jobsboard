package com.sameera.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.*
import cats.implicits.*
import cats.effect.*
import com.sameera.jobsboard.domain.job.*
import com.sameera.jobsboard.http.responses.FailureResponse

import scala.collection.mutable
import java.util.UUID

class JobRoutes[F[_]: Concurrent] private extends Http4sDsl[F] {

  // database
  private val database = mutable.Map[UUID, Job]()

  // POST  /jobs?offset=x&limit=y {filters} // TODO add queryparams and filter later
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok(database.values)
  }

  // GET jobs/{uuid}
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    database.get(id) match {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"job with id $id not found"))
    }
  }

  // POST /jobs {jobInfo}

  private def createJob(jobInfo: JobInfo): F[Job] = Job(
    id = UUID.randomUUID(),
    date = System.currentTimeMillis(),
    ownerEmail = "TODO@Jobsboard.com",
    jobInfo = jobInfo,
    active = true
  ).pure[F]

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        jobInfo <- req.as[JobInfo]
        job     <- createJob(jobInfo)
        resp    <- Created(job.id)
      } yield resp
  }

  // PUT /jobs/uuid {jobInfo}
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo]
            _       <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
            resp    <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot update job $id : not found"))
      }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ DELETE -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            _    <- database.remove(id).pure[F]
            resp <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id : not found"))
      }
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )

}

object JobRoutes {
  def apply[F[_]: Concurrent] = new JobRoutes[F]
}
