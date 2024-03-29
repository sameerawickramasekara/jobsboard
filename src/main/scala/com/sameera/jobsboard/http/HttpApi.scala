package com.sameera.jobsboard.http

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.*
import com.sameera.jobsboard.http.routes.{HealthRoutes, JobRoutes}
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.typelevel.log4cats.Logger

class HttpApi[F[_]: Concurrent: Logger] private {

  private val healthRoutes = HealthRoutes[F].routes
  private val jobRoutes    = JobRoutes[F].routes

  val endpoints = Router(
    "api" -> (healthRoutes <+> jobRoutes)
  )

}

object HttpApi {
  def apply[F[_]: Concurrent: Logger] = new HttpApi[F]
}
