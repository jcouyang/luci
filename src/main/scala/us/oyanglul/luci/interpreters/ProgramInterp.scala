package us.oyanglul.luci
package interpreters

import cats._
import scala.util.Properties._
import cats.data.{EitherK, Kleisli}

trait ProgramInterp extends ReaderTInterp with DoobieInterp with IoInterp {

  implicit def programInterpreter[E[_], C, F[_], G[_]](
      implicit foldf: Interpreter[E, F, C],
      foldr: Interpreter[E, G, C]): Interpreter[E, EitherK[F, G, ?], C] =
    new Interpreter[E, EitherK[F, G, ?], C] {
      def translate =
        Lambda[EitherK[F, G, ?] ~> Kleisli[E, C, ?]](
          _.fold(foldf.translate, foldr.translate))
    }

  private[interpreters] def mandatoryEnv(name: String) =
    envOrNone(name)
      .toRight(List(s"Please specify env $name"))
}
