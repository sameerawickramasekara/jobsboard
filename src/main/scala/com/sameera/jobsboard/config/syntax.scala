package com.sameera.jobsboard.config

import cats.MonadThrow
import pureconfig.ConfigSource
import pureconfig.ConfigReader
import cats.implicits.*
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](using reader: ConfigReader[A], F: MonadThrow[F], tag: ClassTag[A]): F[A] =
      F.pure(source.load[A]).flatMap {
        case Left(error)  => F.raiseError[A](ConfigReaderException(error))
        case Right(value) => F.pure(value)
      }
}
