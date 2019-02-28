package us.oyanglul.luci
package interpreters

import cats.{MonadError, _}
import cats.data._
import cats.data.Kleisli
import cats.effect.IO
import monocle.Lens
import scala.util.Properties._
import cats.arrow.FunctionK
import cats.data.{EitherK, Kleisli}
import doobie.free.connection.ConnectionOp
import doobie.util.transactor.Transactor

trait ProgramInterp {

  // This is kind of eh … we need to interpret F into Kleisli so this is helpful
  implicit class NaturalTransformationOps[F[_], G[_]](nat: F ~> G) {
    def liftK[E] = Lambda[F ~> Kleisli[G, E, ?]](fa => Kleisli(_ => nat(fa)))
  }

  implicit def programInterpreter[E[_], C, F[_], G[_]](
      implicit foldf: Interpreter[E, F, C],
      foldr: Interpreter[E, G, C]): Interpreter[E, EitherK[F, G, ?], C] =
    new Interpreter[E, EitherK[F, G, ?], C] {
      def translate =
        Lambda[EitherK[F, G, ?] ~> Kleisli[E, C, ?]](
          _.fold(foldf.translate, foldr.translate))
    }

  implicit def dbInterp[E[_], C](implicit L: Lens[C, Transactor[E]],
                                 ev: MonadError[E, Throwable]) =
    new Interpreter[E, ConnectionOp, C] {
      def translate = Lambda[ConnectionOp ~> Kleisli[E, C, ?]] { op =>
        Kleisli { ctx =>
          val tx = L.get(ctx)
          tx.exec.apply(tx.interpret(op))
        }
      }
    }

  implicit def ioInterp[C] = new Interpreter[IO, IO, C] {
    def translate = FunctionK.id[IO].liftK[C]
  }

  private[interpreters] def mandatoryEnv(name: String) =
    envOrNone(name)
      .toRight(List(s"Please specify env $name"))
}
