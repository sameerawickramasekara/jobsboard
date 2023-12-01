package com.sameera.jobsboard.http.routes

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*

// By extending from Http4Dsl we remove the need to define the object and then importing from it
// during the route creation
class HealthRoutes[F[_]: Monad] private extends Http4sDsl[F] {

  private val healthRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok("All good")
  }

  val routes: HttpRoutes[F] = Router(
    "/health" -> healthRoute
  )
}

object HealthRoutes {
  def apply[F[_]: Monad] = new HealthRoutes[F]
}
