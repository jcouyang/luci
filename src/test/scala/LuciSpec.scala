package us.oyanglul.luci

import cats.data._
import cats.effect.concurrent.Ref
import cats.effect.{IO, Resource}
import cats.free.Free
import cats.mtl.{FunctorTell, MonadState}
import doobie.free.connection.{ConnectionIO}
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import resources._
import interpreters._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io.{Ok, _}
import org.http4s._
import org.specs2.mutable.Specification
import org.http4s.implicits._
import doobie.implicits._
import scala.concurrent.ExecutionContext.Implicits.global
import monocle.macros.GenLens
import com.olegpy.meow.effects._
import cats.instances.all._
import cats.syntax.all._
import org.http4s.client.dsl.io._

import scala.concurrent.ExecutionContext

class LuciSpec
    extends Specification
    with DatabaseResource
    with interpreters.All {
  implicit val cs = IO.contextShift(ExecutionContext.global)
  val httpClientResource = BlazeClientBuilder[IO](global).resource
  "Luci" >> {
    "Given you have define all types for your program".p.tab

    case class AppContext(transactor: Transactor[IO], http: Client[IO])

    type Eff1[A] =
      EitherK[effects.WriterT, effects.HttpClient[IO, ?], A]
    type Eff2[A] =
      EitherK[ReaderT[IO, ProgramContext, ?], Eff1, A]
    type Eff3[A] = EitherK[IO, Eff2, A]

    type Program[A] = EitherK[ConnectionIO, Eff3, A]
    type ProgramF[A] = Free[Program, A]

    case class ProgramState(someState: String)
    case class Config(token: String)
    case class ProgramContext(teller: FunctorTell[IO, Chain[IO[Unit]]],
                              config: Config,
                              state: MonadState[IO, ProgramState],
                              appContext: AppContext)

    "And a Application".p.tab
    def createApp(implicit ctx: AppContext) = {
      val ping = freeRoute[IO, Program] {
        case _ @GET -> Root =>
          implicit val han = LogHandler.jdkLogHandler
          for {
            config <- Free.liftInject[Program](Kleisli.ask[IO, ProgramContext])
            _ <- Free.liftInject[Program](effects.Debug(s"heheh...$config"))
            _ <- Free.liftInject[Program](for {
              _ <- sql"""insert into test values (4)""".update.run
              // _ <- sql"""insert into test values ('aaa1')""".update.run
            } yield ())
            _ <- Free.liftInject[Program](IO(println(s"im IO...")))
            res <- Free.liftInject[Program](Ok("live"))
          } yield res
      }
      ping.map(runProgram)
    }

    def runProgram[A](program: ProgramF[A])(implicit
                                            ctx: AppContext) = {
      programResource(Ref[IO].of(ProgramState("hehe")),
                      Config("im config...").asRight[Throwable]).use {
        case (logEff, config, state) =>
          implicit val context =
            ProgramContext(logEff.tellInstance,
                           config,
                           state.stateInstance,
                           ctx)
          implicit val lensHttpClient =
            GenLens[ProgramContext](_.appContext.http)
          implicit val lensTell = GenLens[ProgramContext](_.teller)
          implicit val lensTransactor =
            GenLens[ProgramContext](_.appContext.transactor)
          val binary = program foldMap implicitly[
            Interpreter[IO, Program, ProgramContext]].translate
          binary.run(context)
      } unsafeRunSync ()
    }
    def programResource[S, C](stateRef: IO[Ref[IO, S]],
                              validatedConfig: Either[Throwable, C])
      : Resource[IO, (RefLog, C, Ref[IO, S])] = {
      Resource.make {
        for {
          logEff <- Ref.of[IO, Chain[IO[Unit]]](Chain.empty)
          state <- stateRef
          config <- validatedConfig match {
            case Right(config) => IO(config)
            case Left(error)   => IO.raiseError(error)
          }
        } yield (logEff, config, state)
      } {
        case (logEff, _, _) =>
          logEff.get.flatMap(_.toList.sequence_)
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
