package us.oyanglul.luci
package interpreters

import cats._
import cats.data.{EitherK, Kleisli}
import cats.effect.IO

object generic extends HighPriorityImplicits

trait HighPriorityImplicits extends LowPriorityImplicits {
  implicit def highPriorityInterp[E[_], F[_], G[_], H[_], A, B](
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
}

trait LowPriorityImplicits {
  implicit def lowPriorityInter[E[_], F[_], G[_], A, B, C](
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
