package us.oyanglul.luci
package interpreters

import cats.~>
import cats.data.{EitherK, Kleisli}

object generic extends HighPriorityImplicits

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
