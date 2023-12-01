package com.sameera.jobsboard.foundations

import cats.effect.IO.FlatMap

/** Cats crash course, demonstrating most important parts
  */
object Cats {

  /*
  type classes
  - Applicatives
  - Functor
  - FlatMap
  - Monad
  - ApplicativeError/MonadError
   */

  // functor - describes mappable structures
  trait MyFunctor[F[_]] {
    def map[A, B](initialValue: F[A])(f: A => B): F[B]
  }
  // to use it in cats

  import cats.Functor
  import cats.instances.list.* // <- Functor instances for list is here

  val listFunctor: Functor[List] = Functor[List]
  val mappedList                 = listFunctor.map(List(1, 2, 3))(_ + 1)

  // verbose way of using map
  def increment[F[_]](container: F[Int])(using functor: Functor[F]): F[Int] =
    functor.map(container)(_ + 1)

  // by importing cats.syntax.functor.* it exposes map as an extension method for any container that has
  // an implicit functor instance in scope

  import cats.syntax.functor.*

  def increment_v2[F[_]](container: F[Int])(using functor: Functor[F]): F[Int] =
    container.map(_ + 1)

  // Applicative - pure wrap existing values into "wrapper" values

  trait MyApplicative[F[_]] extends Functor[F] {
    def pure[A](value: A): F[A]
  }

  import cats.Applicative

  val applicativeList: Applicative[List] = Applicative[List]
  val aSimpleList: Seq[Int] = applicativeList.pure(
    42
  ) // <- gives us a List with single value 42, here the wrapper type is List

  import cats.syntax.applicative.*

  val aSimpleList_v2 =
    42.pure[
      List
    ] // now pure is an extension method for simple types that can wrap into wrapper types

  // flatMap
  trait MyFlatMap[F[_]] extends Functor[F] {
    def flatMap[A, B](initialValue: F[A])(f: A => F[B]): F[B]
  }

  import cats.FlatMap

  val flatMapList    = FlatMap[List]
  val flatMappedList = flatMapList.flatMap(List(1, 2, 3))(x => List(x, x + 1))

  import cats.syntax.flatMap.*

  def crossProduct[F[_], A, B](containerA: F[A], containerB: F[B])(using
      flatMapper: FlatMap[F]
  ): F[(A, B)] =
    containerA.flatMap(a => containerB.map(b => (a, b)))

  def crossProductV2[F[_]: FlatMap, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] = for {
    a <- containerA
    b <- containerB
  } yield (a, b)

  // Monad - applicative + flatMap

  trait MyMonad[F[_]] extends Applicative[F] with FlatMap[F] {
    override def map[A, B](fa: F[A])(f: A => B): F[B] = flatMap(fa)(a => pure(f(a)))
  }

  import cats.Monad

  val monadList = Monad[List]

  def crossProductV3[F[_]: Monad, A, B](containerA: F[A], containerB: F[B]): F[(A, B)] = for {
    a <- containerA
    b <- containerB
  } yield (a, b)

  // Monads and applicatives can only handle pure computations, if somehow our computation fails
  // it will crash,
  // because of this we have ApplicativeError -  computations that can fail

  trait MyApplicativeError[F[_], E] extends Applicative[F] {
    def raiseError[A](
        error: E
    ): F[A] // here we need a wrapper type A that can store the error value if it occures
    // Try.either good candidates,
    // with either we can have the error channel as something other than throwable

    import cats.ApplicativeError
    // cats.ApplicativeThrow <- specialized for throwable

    type ErrorOr[A] = Either[String, A]
    val applicativeEither            = ApplicativeError[ErrorOr, String]
    val desiredValue: ErrorOr[Int]   = applicativeEither.pure(42)
    val undesiredValue: ErrorOr[Int] = applicativeEither.raiseError("something bad happened")

    import cats.syntax.applicativeError.*

    val failedValueV2: ErrorOr[Int] = "Something bad happened".raiseError

    // MonadError - also has flatMap
    trait MyMonadError[F[_], E] extends ApplicativeError[F, E] with Monad[F]
    import cats.MonadError

    val MonadErrorEither: MonadError[ErrorOr, String]

  }

  def main(args: Array[String]): Unit = {}
}
