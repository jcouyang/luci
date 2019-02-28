package us.oyanglul.luci

import cats.data.{Chain, EitherK}
import cats.effect.concurrent.Ref
import cats.effect.{IO, Resource}
import cats.free.Free
import cats.mtl.FunctorTell
import doobie.free.connection.ConnectionOp
import doobie.util.transactor.Transactor
import resources._
import interpreters._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io._
import org.http4s._
import org.specs2.mutable.Specification
import org.http4s.implicits._

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
    with ReaderWriterInterp
    with HttpClientInterp
    with ProgramInterp {
  implicit val cs = IO.contextShift(ExecutionContext.global)
  val httpClientResource = BlazeClientBuilder[IO](global).resource
  "Luci" >> {
    "Given you have define all types for your program".p.tab

    case class AppContext(transactor: Transactor[IO], http: Client[IO])

    type WriterOrHttp[A] = EitherK[effects.Writer, effects.HttpClient[IO, ?], A]
    type IoOrWriterOrHttp[A] = EitherK[IO, WriterOrHttp, A]

    type Program[A] = EitherK[ConnectionOp, IoOrWriterOrHttp, A]
    type ProgramF[A] = Free[Program, A]

    case class Config(token: String)
    case class ProgramContext(teller: FunctorTell[IO, Chain[IO[Unit]]],
                              config: Config,
                              appContext: AppContext)

    "And a Application".p.tab
    def createApp(implicit ctx: AppContext) = {
      val ping = freeRoute[IO, Program] {
        case _ @GET -> Root => Free.liftInject[Program](Ok("live"))
      }
      ping.map(runProgram)
    }

    def runProgram[A](program: ProgramF[A])(implicit
                                            ctx: AppContext) = {
      programResource(Config("hehe").asRight[Throwable]).use {
        case (logEff, config) =>
          implicit val context =
            ProgramContext(logEff.tellInstance, config, ctx)
          implicit val lensHttpClient =
            GenLens[ProgramContext](_.appContext.http)
          implicit val lensTell = GenLens[ProgramContext](_.teller)
          implicit val lensDbTx =
            GenLens[ProgramContext](_.appContext.transactor)

          val binary = program foldMap implicitly[
            Interpreter[IO, Program, ProgramContext]].translate
          binary.run(context)
      } unsafeRunSync ()
    }
    def programResource[C](
        validatedConfig: Either[Throwable, C]): Resource[IO, (RefLog, C)] = {
      Resource.make {
        for {
          logEff <- Ref.of[IO, Chain[IO[Unit]]](Chain.empty)
          config <- validatedConfig match {
            case Right(config) => IO(config)
            case Left(error)   => IO.raiseError(error)
          }
        } yield (logEff, config)
      } {
        case (logEff, _) =>
          logEff.get.flatMap(_.toList.sequence_)
      }
    }

    "When run Program".p.tab
    val req = GET(Uri.uri("/")) unsafeRunSync ()
    databaseResource
      .use { tx =>
        httpClientResource.use { client =>
          createApp(AppContext(tx, client)).orNotFound(req)
        }
      }
      .unsafeRunSync()
      .status must_== Ok

  }

}
