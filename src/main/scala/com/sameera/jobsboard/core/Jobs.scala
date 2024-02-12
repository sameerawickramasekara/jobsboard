package com.sameera.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*
import com.sameera.jobsboard.domain.job.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*

import java.util.UUID

trait Jobs[F[_]] {
  // algebra
  // CRUD operations

  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]

  def all(): F[List[Job]]

  def findById(id: UUID): F[Option[Job]]

  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]

  def delete(id: UUID): F[Int]

}

/** id: UUID, date: Long, ownerEmail: String, company: String, title: String, description: String,
  * externalUrl: String, remote: Boolean, location: String, salaryLo: Option[Int], salaryHi:
  * Option[Int], currency: Option[String], country: Option[String], tag: Option[List[String]],
  * image: Option[String], seniority: Option[String], other: Option[String] active: Boolean = false
  */
class LiveJobs[F[_]: MonadCancelThrow] private (xa: Transactor[F]) extends Jobs[F] {

  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
         |INSERT INTO jobs(
         |  date,
         |  ownerEmail,
         |  company,
         |  title,
         |  description,
         |  externalUrl,
         |  remote,
         |  location,
         |  salaryLo,
         |  salaryHi,
         |  currency,
         |  country,
         |  tag,
         |  image,
         |  seniority,
         |  other,
         |  active
         |) VALUES(
         |  ${System.currentTimeMillis()},
         |  ${ownerEmail},
         |  ${jobInfo.company},
         |  ${jobInfo.title},
         |  ${jobInfo.description},
         |  ${jobInfo.externalUrl},
         |  ${jobInfo.remote},
         |  ${jobInfo.location},
         |  ${jobInfo.salaryLo},
         |  ${jobInfo.salaryHi},
         |  ${jobInfo.currency},
         |  ${jobInfo.country},
         |  ${jobInfo.tag},
         |  ${jobInfo.image},
         |  ${jobInfo.seniority},
         |  ${jobInfo.other},
         |  false
         |)
         |""".stripMargin.update.withUniqueGeneratedKeys[UUID]("id").transact(xa)

  override def all(): F[List[Job]] =
    sql"""
         |SELECT
         |  id,
         |  date,
         |  ownerEmail,
         |  company,
         |  title,
         |  description,
         |  externalUrl,
         |  remote,
         |  location,
         |  salaryLo,
         |  salaryHi,
         |  currency,
         |  country,
         |  tag,
         |  image,
         |  seniority,
         |  other,
         |  active
         |FROM jobs
         |"""
      .query[Job]
      .to[List]
      .transact(xa)

  override def findById(id: UUID): F[Option[Job]] =
    sql"""
         |SELECT
         |  id,
         |  date,
         |  ownerEmail,
         |  company,
         |  title,
         |  description,
         |  externalUrl,
         |  remote,
         |  location,
         |  salaryLo,
         |  salaryHi,
         |  currency,
         |  country,
         |  tag,
         |  image,
         |  seniority,
         |  other,
         |  active
         |FROM jobs
         |WHERE id=$id
         |""".stripMargin
      .query[Job]
      .option
      .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
         |UPDATE jobs
         |SET(
         |  company = ${jobInfo.company},
         |  title = ${jobInfo.title},
         |  description = ${jobInfo.description},
         |  externalUrl = ${jobInfo.externalUrl},
         |  remote = ${jobInfo.remote},
         |  location = ${jobInfo.location},
         |  salaryLo ${jobInfo.salaryLo},
         |  salaryHi = ${jobInfo.salaryHi},
         |  currency = ${jobInfo.currency},
         |  country = ${jobInfo.country},
         |  tag = ${jobInfo.tag},
         |  image = ${jobInfo.image},
         |  seniority = ${jobInfo.seniority},
         |  other = ${jobInfo.other},
         |)
         |WHERE id = ${id}
         |""".stripMargin.update.run.transact(xa).flatMap { _ => findById(id) }

  override def delete(id: UUID): F[Int] =
    sql"""
         |DELETE FROM job WHERE id = ${id}
         |""".stripMargin.update.run.transact(xa)

}

object LiveJobs {

  given jobRead: Read[Job] = Read[
    (
        UUID,
        Long,
        String,
        String,
        String,
        String,
        String,
        Boolean,
        String,
        Option[Int],
        Option[Int],
        Option[String],
        Option[String],
        Option[List[String]],
        Option[String],
        Option[String],
        Option[String],
        Boolean
    )
  ].map {
    case (
          id: UUID,
          date: Long,
          ownerEmail: String,
          company: String,
          title: String,
          description: String,
          externalUrl: String,
          remote: Boolean,
          location: String,
          salaryLo: Option[Int] @unchecked,
          salaryHi: Option[Int] @unchecked,
          currency: Option[String] @unchecked,
          country: Option[String] @unchecked,
          tag: Option[List[String]] @unchecked,
          image: Option[String] @unchecked,
          seniority: Option[String] @unchecked,
          other: Option[String] @unchecked,
          active: Boolean
        ) =>
      Job(
        id = id,
        date = date,
        ownerEmail = ownerEmail,
        jobInfo = JobInfo(
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tag,
          image,
          seniority,
          other
        ),
        active = active
      )
  }

  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure
}
