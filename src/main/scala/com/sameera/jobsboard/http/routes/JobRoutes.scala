package com.sameera.jobsboard.http.routes

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import cats.*
import cats.implicits.*

class JobRoutes[F[_]: Monad] private extends Http4sDsl[F] {

  // POST  /jobs?offset=x&limit=y {filters} // TODO add queryparams and filter later
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok("TODO")
  }

  // GET jobs/{uuid}
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    Ok(s"TODO find job for $id")
  }

  // POST /jobs {jobInfo}
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root / "create" =>
    Ok("TODO")
  }

  // PUT /jobs/uuid {jobInfo}
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case PUT -> Root / UUIDVar(id) =>
    Ok(s"TODO Update job for ${id}")
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(id) => Ok(s"TODO Delete job for ${id}")
  }

  val routes: HttpRoutes[F] = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )

}

object JobRoutes {
  def apply[F[_]: Monad] = new JobRoutes[F]
}
