package us.oyanglul.luci
package interpreters

import cats.~>
import cats.data.{EitherK, Kleisli}
import doobie.util.transactor.Transactor
import org.http4s.client.Client
import shapeless._
import cats.effect.IO
import effects._

object generic extends HighPriorityImplicits

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

trait LowPriorityInterpreter[E[_]] {
  def compile[F1[_], F2[_], A](eff: EitherK[F1, F2, A])(implicit
                                                        ev1: Compiler[F1, E],
                                                        ev2: Compiler[F2, E]) =
    compile2[F1, F2].compile(eff)

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
object Compiler extends GenericInterpreter[IO]

trait GenericInterpreter[E[_]] extends LowPriorityInterpreter[E] {
  def compile[F1[_], F2[_], F3[_], A, Eff[A] <: EitherK[F2, F3, A]](
      eff: EitherK[F1, EitherK[F2, F3, ?], A])(
      implicit
      ev1: Lazy[Compiler[F1, E]],
      ev2: Compiler[EitherK[F2, F3, ?], E]) =
    compileN[F1, F2, F3].compile(eff)

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

trait HighPriorityImplicits extends LowPriorityImplicits {
  implicit def highPriorityInterp[E[_], F[_], G[_], H[_], A, B](
      implicit foldl: F ~> Kleisli[E, A, ?],
      ev1: B <:< A,
      foldr: EitherK[G, H, ?] ~> Kleisli[E, B, ?],
  ): EitherK[F, EitherK[G, H, ?], ?] ~> Kleisli[E, B, ?] =
    Lambda[EitherK[F, EitherK[G, H, ?], ?] ~> Kleisli[E, B, ?]] { et =>
      val convl = foldl andThen Lambda[Kleisli[E, A, ?] ~> Kleisli[E, B, ?]] {
        _.local(ev1)
      }
      et.fold(convl, foldr)
    }
}

trait LowPriorityImplicits {
  implicit def lowPriorityInter[E[_], F[_], G[_], A, B, C](
      implicit foldl: F ~> Kleisli[E, A, ?],
      ev1: C <:< A,
      foldr: G ~> Kleisli[E, B, ?],
      ev2: C <:< B,
  ): EitherK[F, G, ?] ~> Kleisli[E, C, ?] =
    Lambda[EitherK[F, G, ?] ~> Kleisli[E, C, ?]] { et =>
      val convl = foldl andThen Lambda[Kleisli[E, A, ?] ~> Kleisli[E, C, ?]] {
        _.local(ev1)
      }
      val convr = foldr andThen Lambda[Kleisli[E, B, ?] ~> Kleisli[E, C, ?]] {
        _.local(ev2)
      }
      et.fold(convl, convr)
    }
}

trait DebugInterp {
  @scala.annotation.implicitNotFound(
    """
Sorry, luci cannot find ${F} ~> Kleisli[${E}, ${A}, ?]
Please make sure:
1. You have implicit value of type `${F} ~> Kleisli[${E}, T, ?]` avaiable in context
2. ${A} extends your T in `${F} ~> Kleisli[${E}, T, ?]`
3. still won't compile? try `scalacOptions += "-Xlog-implicits"`
""")
  type CanInterp[F[_], E[_], A] = F ~> Kleisli[E, A, ?]

  implicit def debugInterp[E[_], F[_], A, B](
      implicit foldl: F ~> Kleisli[E, A, ?],
      ev1: B <:< A
  ): CanInterp[F, E, B] = ???
}

object debug extends DebugInterp

private trait ShapeLessTest
    extends Http4sClientCompiler[IO]
    with DoobieCompiler[IO]
    with IoCompiler[IO] {
  import Compiler._
  import doobie.free.connection.ConnectionIO
  import cats.data._
  import cats.effect.IO
  trait Config

  type Program[A] = Eff3[
    Http4sClient[IO, ?],
//    WriterT[IO, Chain[String], ?],
//    ReaderT[IO, Config, ?],
    IO,
    ConnectionIO,
//    StateT[IO, Int, ?],
    A
  ]

  val app = cats.free.Free.liftInject[Program](IO("hehe"))
  type ProgramBin[A] = Kleisli[
    IO,
    (org.http4s.client.Client[cats.effect.IO] :: shapeless.HNil) :: shapeless.HNil :: (doobie.util.transactor.Transactor[
      cats.effect.IO] :: shapeless.HNil) :: shapeless.HNil,
    A]
  app
    .foldMap(Lambda[Program ~> ProgramBin](Compiler.compile(_)))
    .run(((null: Client[IO]) :: Unit :: (null: Transactor[IO]) :: HNil)
      .map(coflatten))

  Compiler
    .compile(
      EitherK.rightc[Http4sClient[IO, ?], EitherK[ConnectionIO, IO, ?], String](
        EitherK.rightc[ConnectionIO, IO, String](IO("12"))))
    .run(((null: Client[IO]) :: (null: Transactor[IO]) :: Unit :: HNil)
      .map(coflatten))

}
private trait Test {
  import effects._
  import doobie.free.connection.ConnectionIO
  import cats.data._
  import cats.effect.IO
  import interpreters.all._
  trait Config

  type Program[A] = Eff6[
    Http4sClient[IO, ?],
    WriterT[IO, Chain[String], ?],
    ReaderT[IO, Config, ?],
    IO,
    ConnectionIO,
    StateT[IO, Int, ?],
    A
  ]

  trait ProgramContext
      extends WriterTEnv[IO, Chain[String]]
      with StateTEnv[IO, Int]
      with HttpClientEnv[IO]
      with DoobieEnv[IO]
      with Config

  import debug._
  implicitly[CanInterp[ConnectionIO, IO, ProgramContext]]
  implicitly[CanInterp[Http4sClient[IO, ?], IO, ProgramContext]]
}
