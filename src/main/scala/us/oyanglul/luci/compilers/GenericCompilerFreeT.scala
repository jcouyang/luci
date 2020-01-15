package us.oyanglul.luci
package freetcompilers

import cats.{Monad, ~>}
import cats.data.{EitherK, Kleisli}
import cats.free.FreeT
import shapeless._
import compilers.Compiler

trait LowPriorityGenericCompiler[E[_]] {
  def compile[F1[_], F2[_], R1, R2, A](prg: FreeT[EitherK[F1, F2, ?], E, A])(
      implicit
      ev0: Monad[E],
      ev1: Compiler.Aux[F1, E, R1],
      ev2: Compiler.Aux[F2, E, R2]) = {
    val pureE = Lambda[E ~> Kleisli[E, R1 :: R2 :: HNil, ?]] {
      Kleisli.liftF(_)
    }
    prg
      .hoist[Kleisli[E, R1 :: R2 :: HNil, ?]](pureE)
      .foldMap(compile2[F1, F2].compile)
  }

  implicit def compile2[A[_], B[_]](implicit ia: Compiler[A, E],
                                    ib: Compiler[B, E]) =
    new Compiler[EitherK[A, B, ?], E] {
      type Env = ia.Env :: ib.Env :: HNil
      val compile = Lambda[EitherK[A, B, ?] ~> Bin] {
        _.run match {
          case Left(a)  => ia.compile(a).local(_.head)
          case Right(b) => ib.compile(b).local(_.tail.head)
        }
      }
    }
}

trait GenericCompiler[E[_]] extends LowPriorityGenericCompiler[E] {
  def compile[F1[_],
              F2[_],
              F3[_],
              R1,
              R2 <: HList,
              A,
              Eff[A] <: EitherK[F2, F3, A]](
      prg: FreeT[EitherK[F1, EitherK[F2, F3, ?], ?], E, A])(
      implicit
      ev0: Monad[E],
      ev1: Lazy[Compiler.Aux[F1, E, R1]],
      ev2: Compiler.Aux[EitherK[F2, F3, ?], E, R2]) = {
    val pureE = Lambda[E ~> Kleisli[E, R1 :: R2, ?]] {
      Kleisli.liftF(_)
    }
    prg
      .hoist[Kleisli[E, R1 :: R2, ?]](pureE)
      .foldMap(compileN[F1, F2, F3].compile)
  }

  implicit def compileN[A[_], B[_], C[_]](implicit
                                          ia: Lazy[Compiler[A, E]],
                                          ib: Compiler[EitherK[B, C, ?], E]) =
    new Compiler[EitherK[A, EitherK[B, C, ?], ?], E] {
      type Env = ia.value.Env :: ib.Env
      val compile =
        Lambda[EitherK[A, EitherK[B, C, ?], ?] ~> Bin] {
          _.run match {
            case Left(a)  => ia.value.compile(a).local(_.head)
            case Right(b) => ib.compile(b).local(_.tail)
          }
        }
    }
}
