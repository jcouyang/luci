package us.oyanglul.luci
package compilers

import cats.{Monad, ~>}
import cats.data.{EitherK, Kleisli}
import cats.free.Free
import doobie.util.transactor.Transactor
import org.http4s.client.Client
import shapeless._
import cats.mtl.{FunctorTell, MonadState}
import effects._

trait CoflattenLowPriority extends Poly1 {
  implicit def anyCase[A]: Case.Aux[A, A :: HNil] = {
    at(a => a :: HNil)
  }
}
object coflatten extends CoflattenLowPriority {
  implicit def unitCase: Case.Aux[Unit.type, HNil] = {
    at(_ => HNil)
  }
  implicit def nilCase[A <: HNil]: Case.Aux[A, A] = {
    at(a => a)
  }
}

trait Compiler[F[_], E[_]] {
  type Env <: HList
  val compile: F ~> Kleisli[E, Env, ?]
}

trait LowPriorityGenericCompiler[E[_]] {
  def compile[F1[_], F2[_], A](prg: Free[EitherK[F1, F2, ?], A])(
      implicit
      ev0: Monad[E],
      ev1: Compiler[F1, E],
      ev2: Compiler[F2, E]) =
    prg.foldMap(compile2[F1, F2].compile)

  implicit def compile2[A[_], B[_]](implicit ia: Compiler[A, E],
                                    ib: Compiler[B, E]) =
    new Compiler[EitherK[A, B, ?], E] {
      type Env = ia.Env :: ib.Env :: HNil
      val compile = Lambda[EitherK[A, B, ?] ~> Kleisli[E, Env, ?]] {
        _.run match {
          case Left(a)  => ia.compile(a).local(_.head)
          case Right(b) => ib.compile(b).local(_.tail.head)
        }
      }
    }
}

trait GenericCompiler[E[_]] extends LowPriorityGenericCompiler[E] {
  def compile[F1[_], F2[_], F3[_], A, Eff[A] <: EitherK[F2, F3, A]](
      prg: Free[EitherK[F1, EitherK[F2, F3, ?], ?], A])(
      implicit
      ev0: Monad[E],
      ev1: Lazy[Compiler[F1, E]],
      ev2: Compiler[EitherK[F2, F3, ?], E]) =
    prg.foldMap(compileN[F1, F2, F3].compile)

  implicit def compileN[A[_], B[_], C[_]](implicit
                                          ia: Lazy[Compiler[A, E]],
                                          ib: Compiler[EitherK[B, C, ?], E]) =
    new Compiler[EitherK[A, EitherK[B, C, ?], ?], E] {
      type Env = ia.value.Env :: ib.Env
      val compile =
        Lambda[EitherK[A, EitherK[B, C, ?], ?] ~> Kleisli[E, Env, ?]] {
          _.run match {
            case Left(a)  => ia.value.compile(a).local(_.head)
            case Right(b) => ib.compile(b).local(_.tail)
          }
        }
    }
}

private trait ShapeLessTest {
  import io._
  import doobie.free.connection.ConnectionIO
  import cats.data._
  import cats.effect.IO
  case class Config()

  type Program[A] = Eff6[
    Http4sClient[IO, ?],
    WriterT[IO, Chain[String], ?],
    ReaderT[IO, Config, ?],
    IO,
    ConnectionIO,
    StateT[IO, Int, ?],
    A
  ]

  val app = cats.free.Free.liftInject[Program](IO("hehe"))
  type ProgramBin[A] = Kleisli[
    IO,
    (Client[cats.effect.IO] :: HNil) :: (FunctorTell[IO, Chain[String]] :: HNil) ::
      (Config :: HNil) :: HNil :: (Transactor[IO] :: HNil) :: (MonadState[
      IO,
      Int] :: HNil) :: HNil,
    A]
  val bin = compile(app)

  bin.run(
    ((null: Client[IO]) :: (null: FunctorTell[IO, Chain[String]]) :: Config() :: Unit :: (null: Transactor[
      IO]) :: (null: MonadState[IO, Int]) :: HNil).map(coflatten))
//  app
//    .foldMap(Lambda[Program ~> ProgramBin](compile(_)))
//    .run(
//      ((null: Client[IO]) :: (null: FunctorTell[IO, Chain[String]]) :: Config() :: Unit :: (null: Transactor[
//        IO]) :: (null: MonadState[IO, Int]) :: HNil)
//        .map(coflatten))
//
//  compile(
//    EitherK.rightc[Http4sClient[IO, ?], EitherK[ConnectionIO, IO, ?], String](
//      EitherK.rightc[ConnectionIO, IO, String](IO("12"))))
//    .run(((null: Client[IO]) :: (null: Transactor[IO]) :: Unit :: HNil)
//      .map(coflatten))

}
