package us.oyanglul.luci
package interpreters

import cats._
import scala.util.Properties._
import cats.data.{EitherK, Kleisli}

trait ProgramInterp extends LowPriorityImplicits {
  implicit def eitherKEmbedded[E[_], F[_], G[_], H[_], A, B](
      implicit foldl: F ~> Kleisli[E, A, ?],
      foldr: EitherK[G, H, ?] ~> Kleisli[E, B, ?],
      ev1: B <:< A
  ): EitherK[F, EitherK[G, H, ?], ?] ~> Kleisli[E, B, ?] =
    Lambda[EitherK[F, EitherK[G, H, ?], ?] ~> Kleisli[E, B, ?]] { et =>
      val convl = (foldl andThen new (Kleisli[E, A, ?] ~> Kleisli[E, B, ?]) {
        def apply[D](kl: Kleisli[E, A, D]): Kleisli[E, B, D] =
          Contravariant[Kleisli[E, ?, D]].contramap(kl)(ev1)
      })
      et.fold(convl, foldr)
    }

  private[interpreters] def mandatoryEnv(name: String) =
    envOrNone(name)
      .toRight(List(s"Please specify env $name"))
}

trait LowPriorityImplicits {
  implicit def programInterpreter[E[_], F[_], G[_], A, B, C](
      implicit foldl: F ~> Kleisli[E, A, ?],
      foldr: G ~> Kleisli[E, B, ?],
      ev1: C <:< A,
      ev2: C <:< B,
  ): EitherK[F, G, ?] ~> Kleisli[E, C, ?] =
    Lambda[EitherK[F, G, ?] ~> Kleisli[E, C, ?]] { et =>
      val convl = (foldl andThen new (Kleisli[E, A, ?] ~> Kleisli[E, C, ?]) {
        def apply[D](kl: Kleisli[E, A, D]): Kleisli[E, C, D] =
          Contravariant[Kleisli[E, ?, D]].contramap(kl)(ev1)
      })
      val convr = (foldr andThen new (Kleisli[E, B, ?] ~> Kleisli[E, C, ?]) {
        def apply[D](kl: Kleisli[E, B, D]): Kleisli[E, C, D] =
          Contravariant[Kleisli[E, ?, D]].contramap(kl)(ev2)
      })
      et.fold(convl, convr)
    }
}
