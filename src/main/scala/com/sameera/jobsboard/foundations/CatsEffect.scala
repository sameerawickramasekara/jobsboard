package com.sameera.jobsboard.foundations

import scala.io.StdIn
import cats.effect.{Concurrent, Deferred, Fiber, GenSpawn, IO, IOApp, MonadCancel, Ref, Resource, Spawn, Sync, Temporal}

import java.io.{File, FileWriter, PrintWriter}
import scala.concurrent.duration.*
import scala.util.Random
import cats.{Defer, MonadError}

import scala.concurrent.ExecutionContext

/**
 * Cats effect crash course, demonstrating most important parts
 */
object CatsEffect extends IOApp.Simple {

  // cats effect is about describing computations as values in a purely functional way
  // IO = data structure describing arbitrary computations, including those that could perform side effects.

  val firstIO: IO[Int] = IO.pure(42)

  val delayedIO: IO[Int] = IO.apply { // (thunk: => A )computation by name, not evaluated immediately, but when required
    println("Im about to produce meaning of life")
    42
  }

  def evaluateIO[A](io: IO[A]): Unit = {
    import cats.effect.unsafe.implicits.global // <- platform on top of which IOs can be evaluated

    val meaningOfLife = io.unsafeRunSync()
    println(s"the result of the effect is $meaningOfLife")
  }

  // transformations (map+ flatMap)

  val improvedMeaningOfLife = firstIO.map(_ * 2)
  val printedMeaningOfLife = firstIO.flatMap(mol => IO(println(mol)))

  // for comprehensions
  def smallProgram(): IO[Unit] = for {
    value1 <- IO(StdIn.readLine())
    value2 <- IO(StdIn.readLine())
    _ <- IO(println(s"$value1 $value2"))
  } yield ()


  //unsafeRunSync is not the ideal way to evaluating IOs
  // IOApp is provided by cats effect
  // IOApp.Simple provides a run method we can use to add code that should be executed


  // IOs can raise/catch errors
  // functional way of try-catch
  val aFailure: IO[Int] = IO.raiseError(new RuntimeException("fail"))
  val dealWithIt = aFailure.handleErrorWith {
    case _: RuntimeException => IO(println("recovered"))
  }

  //fibers = lightweight threads

  val delayedPrint = IO.sleep(1.seconds) *> IO(println(Random.nextInt(100)))
  val manyPrints = for {
    a <- delayedPrint
    b <- delayedPrint
  } yield ()

  val manyPrints2 = for {
    fib1 <- delayedPrint.start
    fib2 <- delayedPrint.start
    _ <- fib1.join
    _ <- fib2.join
  } yield ()

  val cancelledFiber = for {
    fib <- delayedPrint.onCancel(IO(println("Im being cancelled"))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()


  //uncancelable
  val ignoredCancellation = for {
    fib <- IO.uncancelable(_ => delayedPrint.onCancel(IO(println("Im being cancelled")))).start
    _ <- IO.sleep(500.millis) *> IO(println("cancelling fiber")) *> fib.cancel
    _ <- fib.join
  } yield ()
  //cancelling fiber
  //83

  //resources <- glorified IOs with functions for cleaning up

  val readingResource = Resource.make(
    IO(scala.io.Source.fromFile("src/main/scala/com/sameera/jobsboard/foundations/CatsEffect.scala"))
  )(source => IO(println("closing source")) *> IO(source.close()))

  val readingEffect = readingResource.use {
    source => IO(source.getLines().foreach(println))
  }

  //compose resource
  val copiedFileResource = Resource.make(
    IO(new PrintWriter(new FileWriter(new File("src/main/resources/dumpedFile.scala"))))
  )(writer => IO(println("closing duplicate file")) *> IO(writer.close()))

  val compositeResource = for {
    source <- readingResource
    destination <- copiedFileResource
  } yield (source, destination)

  val copyFileEffect = compositeResource.use {
    case (source, destination) => IO(source.getLines().foreach(destination.println))
  }

  // abstract kind of computations

  //MonadCancel = cancelable computations
  trait MyMonadCancel[F[_], E] extends MonadError[F, E] {

    trait CancellationFlagResetter {
      def apply[A](fa: F[A]): F[A]
    }

    def canceled: F[Unit]

    def uncancelable[A](poll: CancellationFlagResetter => F[A]): F[A]
  }

  val monadCancelIO: MonadCancel[IO, Throwable] = MonadCancel[IO]
  val uncancelableIO = monadCancelIO.uncancelable(_ => IO(42))

  //Spawn = ability to create fibers

  trait MyGenSpawn[F[_], E] extends MonadCancel[F, E] {
    def start[A](fa: F[A]): F[Fiber[F, E, A]] //creates a fiber
    // never, cede, racePair also APIs that we can implement
  }

  trait MySpawn[F[_]] extends GenSpawn[F, Throwable]

  val spawnIO = Spawn[IO]
  val fiber = spawnIO.start(delayedPrint) // creates a fiber, same as delayedPrint.start

  // concurrent = concurrency primitives (atomic references + promises)

  trait MyConcurrent[F[_]] extends Spawn[F] {
    def ref[A](a: A): F[Ref[F, A]]

    def deferred[A]: F[Deferred[F, A]]
  }

  // temporal = ability to suspend computations for a given time
  trait MyTemporal[F[_]] extends Concurrent[F] {
    def sleep(time: FiniteDuration): F[Unit]
  }

  // Sync = ability to suspend synchronous arbitrary expressions in an effect
  trait MySync[F[_]] extends MonadCancel[F, Throwable] with Defer[F] {
    def delay[A](expression: => A): F[A]

    def blocking[A](expression: => A): F[A] // runs on a dedicated clocking thread pool

  }

  //Async = ability to suspend asynchronous computations (i.e on other thread pools) into an effect managed by CE
  trait MyAsync[F[_]] extends Sync[F] with Temporal[F] {
    def executionContext: F[ExecutionContext]

    def async[A](cb: (Either[Throwable, A] => Unit) => F[Option[F[Unit]]]): F[A]
  }


  override def run: IO[Unit] = copyFileEffect

  // main method not needed when extending IOApp
  //  def main(args: Array[String]): Unit = {
  //    evaluateIO(delayedIO)
  //    //Im about to produce meaning of life
  //    //the result of the effect is 42
  //
  //    evaluateIO(smallProgram())
  //    //hello
  //    //world
  //    //hello world
  //    //the result of the effect is ()
  //
  //
  //  }

}
