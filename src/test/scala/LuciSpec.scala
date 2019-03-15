package us.oyanglul.luci

import cats.~>
import cats.Monad
import cats.data._
import cats.effect.concurrent.Ref
import cats.effect.{IO, Resource}
import cats.free.Free
import cats.syntax.all._
import cats.instances.all._
import doobie.free.connection.{ConnectionIO}
import effects.Http4sClient
import doobie.util.log.LogHandler
import doobie.util.transactor.{Transactor}
import resources._

import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io.{Ok, _}
import org.http4s._
import org.specs2.mutable.Specification
import org.http4s.implicits._
import doobie.implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import com.olegpy.meow.effects._
import org.http4s.client.dsl.io._

import scala.concurrent.ExecutionContext
import compilers._
import compilers.all._
import compilers.generic._
import compilers._
import Free.liftInject
class LuciSpec extends Specification with DatabaseResource {
  implicit val cs = IO.contextShift(ExecutionContext.global)
  type FreeRoute[F[_], G[_]] =
    Kleisli[OptionT[F, ?], Request[F], Free[G, Response[F]]]
  def freeRoute[F[_]: Monad, G[_]](
      pf: PartialFunction[Request[F], Free[G, Response[F]]]): FreeRoute[F, G] =
    Kleisli(
      (req: Request[F]) => OptionT(implicitly[Monad[F]].pure(pf.lift(req))))
  type RefLog = Ref[IO, Chain[IO[Unit]]]

  val httpClientResource = BlazeClientBuilder[IO](global).resource
  "Luci" >> {
    "Given you have define all types for your program".p.tab

    case class AppContext(transactor: Transactor[IO], http: Client[IO])

    type Program[A] = Eff7[
      Http4sClient[IO, ?],
      WriterT[IO, Chain[String], ?],
      ReaderT[IO, Config, ?],
      Either[Throwable, ?],
      IO,
      ConnectionIO,
      StateT[IO, Int, ?],
      A
    ]
    type ProgramF[A] = Free[Program, A]

    type ProgramBin[A] = Kleisli[IO, ProgramContext, A]

    case class ProgramState(someState: String)

    trait Config {
      val token: String
    }
    trait ProgramContext
        extends WriterTEnv[IO, Chain[String]]
        with StateTEnv[IO, Int]
        with HttpClientEnv[IO]
        with DoobieEnv[IO]
        with Config

    "And a Application".p.tab
    def createApp(implicit ctx: AppContext) = {
      implicit val han = LogHandler.jdkLogHandler
      val dbOps = for {
        _ <- sql"""insert into test values (4)""".update.run
        _ <- sql"""insert into test values ('aaa1')""".update.run
      } yield ()
      val ping = freeRoute[IO, Program] {
        case _ @GET -> Root =>
          for {
            config <- liftInject[Program](Kleisli.ask[IO, Config])
            state1 <- liftInject[Program](StateT.get[IO, Int])
            _ <- liftInject[Program](StateT.modify[IO, Int](1 + _))
            _ <- liftInject[Program](
              WriterT.tell[IO, Chain[String]](
                Chain.one("config: " + config.token)))
            resOrError <- liftInject[Program](dbOps.attempt)
            _ <- liftInject[Program](
              resOrError.handleError(e => println(s"handle db error $e")))
            _ <- liftInject[Program](IO(println(s"im IO...state: $state1")))
            res <- liftInject[Program](Ok("live"))
          } yield res
      }
      ping.map(runProgram)
    }

    def runProgram[A](program: ProgramF[A])(implicit
                                            ctx: AppContext) = {
      programResource(Ref[IO].of(1), new Config {
        val token = "im config..."
      }.asRight[Throwable]).use {
        case (logEff, _, stateEff) =>
          val binary = program foldMap implicitly[Program ~> ProgramBin]
          // (http4sClientInterp[IO] or (writerInterp[
          //   IO,
          //   Chain[String]] or (readerTInterp[IO, Config] or (eitherInterp[
          //   Throwable,
          //   IO] or (ioInterp or (doobieInterp[IO] or stateTInterp[IO, Int]))))))
          binary.run(new ProgramContext {
            val token = "hehe"
            val stateT = stateEff.stateInstance
            val writerT = logEff.tellInstance
            val http4sClient = ctx.http
            val doobieTransactor = ctx.transactor
          })
      } unsafeRunSync ()
    }
    def programResource[S, C](stateRef: IO[Ref[IO, S]],
                              validatedConfig: Either[Throwable, C])
      : Resource[IO, (Ref[IO, Chain[String]], C, Ref[IO, S])] = {
      Resource.make {
        for {
          logEff <- Ref.of[IO, Chain[String]](Chain.empty)
          state <- stateRef
          config <- validatedConfig match {
            case Right(config) => IO(config)
            case Left(error)   => IO.raiseError(error)
          }
        } yield (logEff, config, state)
      } {
        case (logEff, _, state) =>
          logEff.get.flatMap(log => IO(println(log))) *> state.get.flatMap(
            log => IO(println(log)))
      }
    }

    "When run Program".p.tab
    val req = GET(Uri.uri("/")) unsafeRunSync ()
    databaseResource
      .use { tx =>
        httpClientResource.use { client =>
          implicit val actx = AppContext(tx, client)
          IO(
            createApp
              .orNotFound(req)
              .unsafeRunSync()
              .status must_== Ok)
        }
      } unsafeRunSync ()

  }

}
