package us.oyanglul.luci

import cats.~>
import cats.data._
import cats.effect.concurrent.Ref
import cats.effect.{IO, Resource}
import cats.free.Free
import doobie.free.connection.{ConnectionIO}
// import doobie.util.log.LogHandler
import doobie.util.transactor.{Transactor}
import resources._

import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io.{Ok, _}
import org.http4s._
import org.specs2.mutable.Specification
import org.http4s.implicits._
// import doobie.implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import com.olegpy.meow.effects._
import cats.syntax.all._
import org.http4s.client.dsl.io._

import scala.concurrent.ExecutionContext
import interpreters.all._
import interpreters._
class LuciSpec
    extends Specification
    with DatabaseResource
    with interpreters.ProgramInterp {
  implicit val cs = IO.contextShift(ExecutionContext.global)
  val httpClientResource = BlazeClientBuilder[IO](global).resource
  "Luci" >> {
    "Given you have define all types for your program".p.tab

    case class AppContext(transactor: Transactor[IO], http: Client[IO])

    type Eff1[A] =
      EitherK[WriterT[IO, Chain[String], ?], effects.HttpClient[IO, ?], A]
    type Eff2[A] =
      EitherK[ReaderT[IO, Config, ?], Eff1, A]
    type Eff3[A] = EitherK[IO, Eff2, A]
    type Eff4[A] = EitherK[StateT[IO, Int, ?], Eff3, A]
    type Program[A] = EitherK[ConnectionIO, Eff4, A]
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
      val ping = freeRoute[IO, Program] {
        case _ @GET -> Root =>
          // implicit val han = LogHandler.jdkLogHandler
          for {
            config <- Free.liftInject[Program](Kleisli.ask[IO, Config])
            state1 <- Free.liftInject[Program](StateT.get[IO, Int])
            _ <- Free.liftInject[Program](StateT.modify[IO, Int](1 + _))
            _ <- Free.liftInject[Program](
              WriterT.tell[IO, Chain[String]](
                Chain.one("config: " + config.token)))
            // _ <- Free.liftInject[Program](for {
            // _ <- sql"""insert into test values (4)""".update.run
            // _ <- sql"""insert into test values ('aaa1')""".update.run
            // } yield ())
            _ <- Free.liftInject[Program](
              IO(println(s"im IO...state: $state1")))
            res <- Free.liftInject[Program](Ok("live"))
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
