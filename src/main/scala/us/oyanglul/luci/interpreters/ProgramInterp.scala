package us.oyanglul.luci
package interpreters

import cats.arrow.FunctionK
import cats.~>
import cats.data.{EitherK, Kleisli}
import doobie.util.transactor.Transactor

import org.http4s.client.Client
import shapeless._
import cats.effect.IO
import effects._
import doobie.free.connection.{ConnectionIO}

object generic extends HighPriorityImplicits

trait Interpretable[F[_], E[_]] {
  type Env
  val interp: F ~> Kleisli[E, Env, ?]
}

object Interpretable {
  type Aux[F[_], E[_], R] = Interpretable[F, E] { type Env = R }
}

trait GenericLowPriorityInterp {
  implicit def canInterp2[A[_], B[_]](implicit ia: Interpretable[A, IO],
                                      ib: Interpretable[B, IO]) =
    new Interpretable[EitherK[A, B, ?], IO] {
      type Env = ia.Env :: ib.Env :: HNil
      val interp = Lambda[EitherK[A, B, ?] ~> Kleisli[IO, Env, ?]] {
        _.run match {
          case Left(a)  => ia.interp(a).local(_.select[ia.Env])
          case Right(b) => ib.interp(b).local(_.select[ib.Env])
        }
      }
    }

}
trait GenericInterpreter extends GenericLowPriorityInterp {
  implicit def canInterp3[A[_], B[_], C[_], D <: HList](
      implicit
      ia: Lazy[Interpretable[A, IO]],
      ib: Interpretable.Aux[EitherK[B, C, ?], IO, D]) =
    new Interpretable[EitherK[A, EitherK[B, C, ?], ?], IO] {
      type Env = ia.value.Env :: D
      val interp =
        Lambda[EitherK[A, EitherK[B, C, ?], ?] ~> Kleisli[IO, Env, ?]] {
          _.run match {
            case Left(a)  => ia.value.interp(a).local(_.head)
            case Right(b) => ib.interp(b).local(_.tail)
          }
        }
    }

  implicit def canInterpHttp4sClient =
    new Interpretable[Http4sClient[IO, ?], IO] {
      type Env = Client[IO]
      val interp = new (Http4sClient[IO, ?] ~> Kleisli[IO, Env, ?]) {
        def apply[A](a: Http4sClient[IO, A]) = {
          a match {
            case b @ Expect(request) =>
              implicit val d = b.decoder
              Kleisli(_.expect[A](request))
            case c: GetStatus[IO] =>
              Kleisli(_.status(c.req))
          }
        }
      }
    }

  case class Context[A](value: A)

  implicit def doobieInterp2 =
    new Interpretable[ConnectionIO, IO] {
      type Env = Transactor[IO]
      val interp = new (ConnectionIO ~> Kleisli[IO, Env, ?]) {
        def apply[A](dbops: ConnectionIO[A]) =
          Kleisli { _ =>
            ???
          }
      }
    }

  implicit val ioInterp2 = new Interpretable[IO, IO] {
    type Env = Any
    val interp = FunctionK.id[IO].liftK[Any]
  }
  import doobie.implicits._

  val a = sql"select true".query[Boolean].unique

  case class AA[T](a: T)
  case class BB[T](b: T)

  implicit val canInterpA = new Interpretable[AA, cats.effect.IO] {
    type Env = Int
    val interp = Lambda[AA ~> Kleisli[IO, Env, ?]] {
      case AA(a) => Kleisli(_ => IO(a))
    }
  }
  implicit val canInterpB = new Interpretable[BB, cats.effect.IO] {
    type Env = String
    val interp = Lambda[BB ~> Kleisli[IO, Env, ?]] {
      case BB(b) => Kleisli(_ => IO(b))
    }
  }

  canInterp2[AA, BB].interp(EitherK.rightc(BB("234"))).run(1 :: "2" :: HNil)


    canInterp3[AA,
             BB,
      IO,
             String :: Any  :: HNil]
    .interp(EitherK.rightc(EitherK.leftc(BB("12"))))
    .run(1 :: "2" :: IO("") :: HNil)

    canInterp3[AA,
             BB,
             EitherK[IO, ConnectionIO, ?],
             String :: Any :: Transactor[IO] :: HNil]
    .interp(EitherK.rightc(EitherK.leftc(BB("12"))))
    .run(1 :: "2" :: IO("") :: HNil)


  canInterp2[Http4sClient[IO, ?], BB]
    .interp(EitherK.rightc(BB("234")))
    .run((null: Client[IO]) :: "2" :: HNil)

  canInterp2[ConnectionIO, Http4sClient[IO, ?]]
    .interp(EitherK.leftc(a))
    .run((null: Transactor[IO]) :: (null: Client[IO]) :: HNil)

  canInterp2[Http4sClient[IO, ?], IO]
    .interp(EitherK.rightc[Http4sClient[IO, ?], IO, Boolean](IO(true)))
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
