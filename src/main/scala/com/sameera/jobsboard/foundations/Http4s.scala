package com.sameera.jobsboard.foundations

import cats.*
import cats.effect.*
import cats.implicits.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.*
import org.typelevel.ci.CIString

import java.util.UUID

/**
 * Crash course in http4s library, which is going to be our server
 */
object Http4s extends IOApp.Simple {

  //simulate an http server with students and courses

  type Student = String

  case class Instructor(firstName: String, lastName: String)

  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepository {

    private val catsEffectCourse = Course(
      "78b546fb-0f2e-444a-b445-809c23c373ac",
      "Ultimate scala course",
      2023,
      List("Sameera", "Anusha"),
      "Martin Odersky"
    )

    private val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    def findCourseById(id: UUID): Option[Course] = courses.get(id.toString)
    def findCoursesByInstructor(name: String): List[Course] = courses.values.filter(_.instructorName == name).toList
  }

  // essential REST endpoints
  //GET localhost:8080/courses?instructor=Martin%20Odersky&year=2023
  //GET localhost:8080/courses/78b546fb-0f2e-444a-b445-809c23c373ac/students

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")

  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match {
          case Some(y) => y.fold(
            _ => BadRequest("Parameter year is invalid"),
            year => Ok(courses.filter(_.year == year).asJson)
          )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCourseById(courseId).map(_.students) match {
          case Some(students) => Ok(students.asJson, Header.Raw(CIString("My-custom-header"), "Sameera"))
          case None => NotFound("No course with course id found")
        }

    }
  }

  def healthEndpoint[F[_]:Monad] :HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("All good")
    }
  }

  def routerWithPrefixes = Router(
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  def allRoutes[F[_]: Monad] = courseRoutes <+>    healthEndpoint

  override def run: IO[Unit] = EmberServerBuilder.default[IO]
    .withHttpApp(routerWithPrefixes)
    .build.use(_ => IO.println("Server ready") *> IO.never)

}
