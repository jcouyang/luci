package us.oyanglul.luci
package resources

import cats.effect.{ContextShift, IO, Resource}
import doobie.util.ExecutionContexts
import doobie.hikari._
import doobie.util.ExecutionContexts

import scala.util.Properties._
import scala.util.Try
import cats.syntax.all._

trait DatabaseResource {
  lazy val DB_HOST = envOrElse("DB_HOST", "localhost")
  lazy val DB_PORT = envOrElse("DB_PORT", "5432")
  lazy val DB_NAME = envOrElse("DB_NAME", "postgres")
  lazy val DB_USER = envOrElse("DB_USER", "postgres")
  lazy val DB_PASSWORD = envOrElse("DB_PASSWORD", "")
  lazy val THREAD_POOL =
    envOrNone("DB_THREAD_POOL_NUM")
      .flatMap(x => Try(x.toInt).toOption)
      .getOrElse(32)

  def databaseResource(implicit ctx: ContextShift[cats.effect.IO])
    : Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      te <- ExecutionContexts.cachedThreadPool[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        s"jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}",
        DB_USER,
        DB_PASSWORD,
        ce,
        te,
      )
    } yield xa
}
