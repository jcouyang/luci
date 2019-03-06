package us.oyanglul

import cats.data.{EitherK, Kleisli}
import shapeless._
import cats.~>

package object luci {
  object Gen {
    trait GenericK[F[_]] {
      type Repr[A]
      def to[A](t: F[A]): Repr[A]
      def from[A](f: Repr[A]): F[A]
    }

    type Aux[F[_], R[_]] = GenericK[F] { type Repr[A] = R[A] }

    implicit def genericEitherK[F1[_], F2[_]](implicit gen: GenericK[F2]) =
      new GenericK[EitherK[F1, F2, ?]] {
        type Repr[A] = F1[A] :+: gen.Repr[A] :+: CNil
        def to[A](t: EitherK[F1, F2, A]): Repr[A] = t.run match {
          case Left(a)  => Inl(a)
          case Right(b) => Inr(Inl(gen.to(b)))
        }

        def from[A](r: Repr[A]): EitherK[F1, F2, A] = r match {
          case Inl(a)      => EitherK.leftc(a)
          case Inr(Inl(b)) => EitherK.rightc(gen.from(b))
          case _           => ???
        }
      }
  }

  type CanInterp[F[_], E[_], A] = F ~> Kleisli[E, A, ?]
  type EffCons[F[_], Eff[_], A] = EitherK[F, Eff, A]
  type Eff2[F1[_], F2[_], A] = EitherK[F1, F2, A]
  type Eff3[F1[_], F2[_], F3[_], A] =
    EffCons[F1, Eff2[F2, F3, ?], A]
  type Eff4[F1[_], F2[_], F3[_], F4[_], A] =
    EffCons[F1, Eff3[F2, F3, F4, ?], A]
  type Eff5[F1[_], F2[_], F3[_], F4[_], F5[_], A] =
    EffCons[F1, Eff4[F2, F3, F4, F5, ?], A]
  type Eff6[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], A] =
    EffCons[F1, Eff5[F2, F3, F4, F5, F6, ?], A]
  type Eff7[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], A] =
    EffCons[F1, Eff6[F2, F3, F4, F5, F6, F7, ?], A]
  type Eff8[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], A] =
    EffCons[F1, Eff7[F2, F3, F4, F5, F6, F7, F8, ?], A]
  type Eff9[F1[_], F2[_], F3[_], F4[_], F5[_], F6[_], F7[_], F8[_], F9[_], A] =
    EffCons[F1, Eff8[F2, F3, F4, F5, F6, F7, F8, F9, ?], A]
  type Eff10[F1[_],
             F2[_],
             F3[_],
             F4[_],
             F5[_],
             F6[_],
             F7[_],
             F8[_],
             F9[_],
             F10[_],
             A] = EffCons[F1, Eff9[F2, F3, F4, F5, F6, F7, F8, F9, F10, ?], A]
  type Eff11[F1[_],
             F2[_],
             F3[_],
             F4[_],
             F5[_],
             F6[_],
             F7[_],
             F8[_],
             F9[_],
             F10[_],
             F11[_],
             A] =
    EffCons[F1, Eff10[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, ?], A]
  type Eff12[F1[_],
             F2[_],
             F3[_],
             F4[_],
             F5[_],
             F6[_],
             F7[_],
             F8[_],
             F9[_],
             F10[_],
             F11[_],
             F12[_],
             A] =
    EffCons[F1, Eff11[F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, ?], A]

}
