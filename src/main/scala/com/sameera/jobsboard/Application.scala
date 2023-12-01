package com.sameera.jobsboard

import cats.Monad
import cats.effect.{IO, IOApp}
import com.sameera.jobsboard.config.*
import com.sameera.jobsboard.config.syntax.*
import cats.implicits.*
import com.sameera.jobsboard.foundations.Http4s.courseRoutes
import com.sameera.jobsboard.http.routes.HealthRoutes
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{->, /}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Origin.Host
import org.http4s.server.Router
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

/** 1- Add plain health endpoint 2 - Add minimal configuration 3 - basic http server layout
  */

object Application extends IOApp.Simple {

  val configSource = ConfigSource.default.load[EmberConfig]

  override def run: IO[Unit] = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
    EmberServerBuilder
      .default[IO]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(HealthRoutes[IO].routes.orNotFound)
      .build
      .use(_ => IO.println("Server ready") *> IO.never)
  }
}
