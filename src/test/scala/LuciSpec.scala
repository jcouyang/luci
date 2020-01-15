package us.oyanglul.luci

import cats.Monad
import cats.data._
import cats.effect.concurrent.Ref
import cats.effect.{IO, Resource}
import cats.free.Free
import cats.syntax.all._
import doobie.free.connection.ConnectionIO
import effects._
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor
import resources._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io.{Ok, _}
import org.http4s._
import org.specs2.mutable.Specification
import org.http4s.implicits._
import doobie.implicits._
import fs2._
import scala.concurrent.ExecutionContext.Implicits.global
import com.olegpy.meow.effects._
import org.http4s.client.dsl.io._

import scala.concurrent.ExecutionContext
import compilers.io._
import Free.{liftInject => free}
import shapeless._
import compilers.coflatten

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
    type Program[A] = Eff8[
      Rescue[Http4sClient[IO, ?], ?],
      Writer[Chain[String], ?],
      ReaderT[IO, Config, ?],
      IO,
      ConnectionIO,
      State[Int, ?],
      EitherT[IO, Throwable, ?],
      Fs2[Http4sClient[IO, ?], IO, ?],
      A
    ]

    type ProgramF[A] = Free[Program, A]

    case class ProgramState(someState: String)

    case class Config(token: String)

    "And a Application".p.tab
    def createApp(implicit ctx: AppContext) = {
      implicit val han = LogHandler.jdkLogHandler
      val dbOps = for {
        _ <- sql"""select true""".query[Boolean].unique
        // _ <- sql"""insert into test values ('aaa1')""".update.run
      } yield ()
      val ping = freeRoute[IO, Program] {
        case _ @GET -> Root =>
          for {
            config <- free[Program](Kleisli.ask[IO, Config])
            request1: Http4sClient[IO, Status] = GetStatus[IO](
              GET(Uri.uri("https://mockbin.org/delay/8000")))

            request2: Http4sClient[IO, String] = Expect[IO, String](
              GET(Uri.uri("http://localhost:8888")))
            resp <- free[Program](
              Attempt(request2): Rescue[Http4sClient[IO, ?],
                                        Either[Throwable, String]])
            stream <- free[Program](
              StreamEmits(List(request1, request1)): Fs2[Http4sClient[IO, ?],
                                                         IO,
                                                         Stream[IO, Status]])
            statuses <- free[Program](stream.compile.toList)
            _ <- free[Program](
              IO(println(
                s"------------\n.Http4s...resp: $resp...status: $statuses")))
            _ <- free[Program](State.modify[Int](1 + _))
            _ <- free[Program](State.modify[Int](1 + _))
            _ <- free[Program](
              Writer.tell[Chain[String]](Chain.one("config: " + config.token)))
            resOrError <- free[Program](dbOps.attempt)
            _ <- free[Program](EitherT(IO(resOrError)))
            state <- free[Program](State.get[Int])
            _ <- free[Program](IO(println(s"im IO...state: $state")))
            res <- free[Program](Ok("live"))
          } yield res
      }
      ping.map(runProgram)
    }

    def runProgram[A](program: ProgramF[A])(implicit
                                            ctx: AppContext) = {
      programResource(Ref[IO].of(1), Config("im config").asRight[Throwable])
        .use {
          case (logEff, config, stateEff) =>
            val args =
              (ctx.http ::
                logEff.tellInstance ::
                config ::
                Unit ::
                ctx.transactor ::
                stateEff.stateInstance ::
                Unit ::
                (2 :: ctx.http :: HNil) :: HNil)
                .map(coflatten)

            val binary = compile(program)
            binary.run(args)

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
