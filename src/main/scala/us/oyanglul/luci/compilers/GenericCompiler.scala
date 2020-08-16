package us.oyanglul.luci
package compilers

import cats.{Monad, ~>}
import cats.data.{EitherK, Kleisli}
import cats.free.Free
import cats.free.FreeT
import shapeless._

trait Compiler[F[_], E[_]] {
  type Env
  type Bin[A] = Kleisli[E, Env, A]
  val compile: F ~> Bin
}

object Compiler {
  type Aux[F[_], E[_], R] = Compiler[F, E] { type Env = R }
}

trait LowPriorityGenericCompiler[E[_]] {
  def compile[F1[_], F2[_], A](
      prg: Free[EitherK[F1, F2, *], A]
  )(implicit ev0: Monad[E], ev1: Compiler[F1, E], ev2: Compiler[F2, E]) =
    prg.foldMap(compile2[F1, F2].compile)

  def compile[F1[_], F2[_], R1, R2, A](
      prg: FreeT[EitherK[F1, F2, *], E, A]
  )(implicit ev0: Monad[E], ev1: Compiler.Aux[F1, E, R1], ev2: Compiler.Aux[F2, E, R2]) = {
    prg
      .hoist(Lambda[E ~> Kleisli[E, R1 :: R2 :: HNil, *]] {
        Kleisli.liftF(_)
      })
      .foldMap(compile2[F1, F2].compile)
  }

  implicit def compile2[A[_], B[_]](implicit ia: Compiler[A, E], ib: Compiler[B, E]) =
    new Compiler[EitherK[A, B, *], E] {
      type Env = ia.Env :: ib.Env :: HNil
      val compile = Lambda[EitherK[A, B, *] ~> Kleisli[E, Env, *]] {
        _.run match {
          case Left(a)  => ia.compile(a).local(_.head)
          case Right(b) => ib.compile(b).local(_.tail.head)
        }
      }
    }
}

trait GenericCompiler[E[_]] extends LowPriorityGenericCompiler[E] {
  // compile Free monad
  def compile[F1[_], F2[_], F3[_], R1 <: HList, A, Eff[A] <: EitherK[F2, F3, A]](
      prg: Free[EitherK[F1, EitherK[F2, F3, *], *], A]
  )(implicit
      ev0: Monad[E],
      compiler: Compiler.Aux[EitherK[F1, EitherK[F2, F3, *], *], E, R1]
  ): Kleisli[E, R1, A] =
    prg.foldMap(compiler.compile)

  // compile FreeT monad
  def compile[F1[_], F2[_], F3[_], R2 <: HList, A, Eff[A] <: EitherK[F2, F3, A]](
      prg: FreeT[EitherK[F1, EitherK[F2, F3, *], *], E, A]
  )(implicit ev0: Monad[E], compiler: Compiler.Aux[EitherK[F1, EitherK[F2, F3, *], *], E, R2]) = {
    prg
      .hoist(Lambda[E ~> Kleisli[E, R2, *]] {
        Kleisli.liftF(_)
      })
      .foldMap(compiler.compile)
  }

  implicit def compileN[A[_], B[_], C[_], R1, R2 <: HList](implicit
      ia: Lazy[Compiler.Aux[A, E, R1]],
      ib: Compiler.Aux[EitherK[B, C, *], E, R2]
  ): Compiler.Aux[EitherK[A, EitherK[B, C, *], *], E, R1 :: R2] =
    new Compiler[EitherK[A, EitherK[B, C, *], *], E] {
      type Env = ia.value.Env :: ib.Env
      val compile =
        Lambda[EitherK[A, EitherK[B, C, *], *] ~> Kleisli[E, Env, *]] {
          _.run match {
            case Left(a)  => ia.value.compile(a).local(_.head)
            case Right(b) => ib.compile(b).local(_.tail)
          }
        }
    }
}
