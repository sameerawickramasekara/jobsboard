package com.sameera.jobsboard.foundations

import cats.effect.{IO, IOApp, MonadCancelThrow}
import doobie.util.transactor.Transactor
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

import doobie.implicits.*

object Doobie extends IOApp.Simple {

  case class Student(id: Int, name: String)

  // first we setup postgres, and we have student database
  // we need a transactor to connect to the database

  val xa: Transactor[IO] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // jdbc connector
    "jdbc:postgresql:demo",  // database URL
    "docker",
    "docker"
  )

  def findAllStudentNames: IO[List[String]] = {
    val query  = sql"select name from students".query[String]
    val action = query.to[List]

    action.transact(xa)
  }

  def saveStudent(id: Int, name: String): IO[Int] = {
    val query  = sql"insert into students(id, name) values($id,$name)"
    val action = query.update.run
    action.transact(xa)
  }

  def findStudentsByInitial(letter: String): IO[List[Student]] = {
    val selectPart = fr"select id,name"
    val fromPart   = fr"from students"
    val wherePart  = fr"where left(name,1) = $letter"
    val statement  = selectPart ++ fromPart ++ wherePart
    val action     = statement.query[Student].to[List]
    action.transact(xa)
  }

  //  organize code

  trait Students[F[_]] {
    def findById(id: Int): F[Option[Student]]

    def findAll: F[List[Student]]

    def create(name: String): F[Int]
  }

  object Students {
    def make[F[_]: MonadCancelThrow](xa: Transactor[F]): Students[F] = new Students[F] {

      override def findAll: F[List[Student]] =
        sql"select id,name from students".query[Student].to[List].transact(xa)

      override def findById(id: Int): F[Option[Student]] =
        sql"select id,name from students where id=$id".query[Student].option.transact(xa)

      override def create(name: String): F[Int] =
        sql"insert into students(name) values($name)".update
          .withUniqueGeneratedKeys[Int]("id")
          .transact(xa)
    }
  }

  val postgresResouce = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](16)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver", // jdbc connector
      "jdbc:postgresql:demo",  // database URL
      "docker",
      "docker",
      ce
    )
  } yield xa

  val smallProgram: IO[Unit] = postgresResouce.use { xa =>
    val studentsRepo = Students.make[IO](xa)
    for {
      id      <- studentsRepo.create("Sameera")
      sameera <- studentsRepo.findById(id)
      _       <- IO.println(s"Student name is $sameera")
    } yield ()
  }

  override def run: IO[Unit] = smallProgram
  //    findStudentsByInitial("S").map(println)
  //    saveStudent(3,"Alice").map(println)
}
