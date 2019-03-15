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

trait Compiler[F[_], E[_]] {
  type Env <: HList
  val compile: F ~> Kleisli[E, Env, ?]
}

trait LowPriorityInterpreter {
  def apply[F1[_], F2[_], A](eff: EitherK[F1, F2, A])(implicit
                                                      ev1: Compiler[F1, IO],
                                                      ev2: Compiler[F2, IO]) =
    compile2[F1, F2].compile(eff)

  implicit def compile2[A[_], B[_]](implicit ia: Compiler[A, IO],
                                    ib: Compiler[B, IO]) =
    new Compiler[EitherK[A, B, ?], IO] {
      type Env = ia.Env :: ib.Env :: HNil
      val compile = Lambda[EitherK[A, B, ?] ~> Kleisli[IO, Env, ?]] {
        _.run match {
          case Left(a)  => ia.compile(a).local(_.head)
          case Right(b) => ib.compile(b).local(_.tail.head)
        }
      }
    }
}
object Compiler extends GenericInterpreter {
  def apply[F1[_], F2[_], F3[_], A, Eff[A] <: EitherK[F2, F3, A]](
      eff: EitherK[F1, EitherK[F2, F3, ?], A])(
      implicit
      ev1: Lazy[Compiler[F1, IO]],
      ev2: Compiler[EitherK[F2, F3, ?], IO]) =
    compile3[F1, F2, F3].compile(eff)

  implicit def compile3[A[_], B[_], C[_]](implicit
                                          ia: Lazy[Compiler[A, IO]],
                                          ib: Compiler[EitherK[B, C, ?], IO]) =
    new Compiler[EitherK[A, EitherK[B, C, ?], ?], IO] {
      type Env = ia.value.Env :: ib.Env
      val compile =
        Lambda[EitherK[A, EitherK[B, C, ?], ?] ~> Kleisli[IO, Env, ?]] {
          _.run match {
            case Left(a)  => ia.value.compile(a).local(_.head)
            case Right(b) => ib.compile(b).local(_.tail)
          }
        }
    }
}

trait GenericInterpreter extends LowPriorityInterpreter {

  implicit def canInterpHttp4sClient =
    new Compiler[Http4sClient[IO, ?], IO] {
      type Env = Client[IO] :: HNil
      val compile = new (Http4sClient[IO, ?] ~> Kleisli[IO, Env, ?]) {
        def apply[A](a: Http4sClient[IO, A]) = {
          a match {
            case b @ Expect(request) =>
              implicit val d = b.decoder
              Kleisli(_.head.expect[A](request))
            case c: GetStatus[IO] =>
              Kleisli(_.head.status(c.req))
          }
        }
      }
    }

  case class Context[A](value: A)

  implicit def doobieInterp2 =
    new Compiler[ConnectionIO, IO] {
      type Env = Transactor[IO] :: HNil
      val compile = new (ConnectionIO ~> Kleisli[IO, Env, ?]) {
        def apply[A](dbops: ConnectionIO[A]) =
          Kleisli { _ =>
            ???
          }
      }
    }

  implicit val ioInterp2 = new Compiler[IO, IO] {
    type Env = HNil
    val compile = FunctionK.id[IO].liftK[Env]
  }
  import doobie.implicits._

  val a = sql"select true".query[Boolean].unique

  case class AA[T](a: T)
  case class BB[T](b: T)

  implicit val canInterpA = new Compiler[AA, cats.effect.IO] {
    type Env = Int :: HNil
    val compile = Lambda[AA ~> Kleisli[IO, Env, ?]] {
      case AA(a) => Kleisli(_ => IO(a))
    }
  }
  implicit val canInterpB = new Compiler[BB, cats.effect.IO] {
    type Env = String :: HNil
    val compile = Lambda[BB ~> Kleisli[IO, Env, ?]] {
      case BB(b) => Kleisli(_ => IO(b))
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

private trait ShapeLess extends GenericInterpreter {
  val app = EitherK.rightc[AA, BB, String](BB("234"))
  Compiler(app)
    .run((1 :: "2" :: HNil).map(hoho))

  Compiler(
    EitherK.rightc[AA, EitherK[BB, IO, ?], String](
      EitherK.leftc[BB, IO, String](BB("12"))))
    .run((1 :: "2" :: Unit :: HNil).map(hoho))

  val iii = implicitly[
    Compiler[EitherK[AA, EitherK[BB, EitherK[IO, ConnectionIO, ?], ?], ?], IO]]
    .compile(EitherK.rightc(EitherK.leftc(BB("12"))))

  // implicitly[Interpretable.Aux[EitherK[BB, EitherK[IO, ConnectionIO, ?], ?],
  //                              IO,
  //                              String :: Any :: Transactor[IO] :: HNil]]

  val aaa = 1 :: "2" :: Unit :: (null: Transactor[IO]) :: HNil

  trait hehe extends Poly1 {
    implicit def anyCase[A]: Case.Aux[A, A :: HNil] = {
      at(a => a :: HNil)
    }

  }

  object hoho extends hehe {
    implicit def sCase: Case.Aux[Unit.type, HNil] = {
      at(_ => HNil)
    }
    implicit def nilCase[A <: HNil]: Case.Aux[A, A] = {
      at(a => a)
    }

  }
  val hhhh = aaa.map(hoho)
  // canInterp3[AA, BB, EitherK[IO, ConnectionIO, ?]]
  //   .interp(EitherK.rightc(EitherK.leftc(BB("12"))))
  //   .run(hhhh)

  Compiler(EitherK.rightc[Http4sClient[IO, ?], BB, String](BB("234")))
    .run(((null: Client[IO]) :: "2" :: HNil).map(hoho))

  compile2[ConnectionIO, Http4sClient[IO, ?]]
    .compile(EitherK.leftc(a))
    .run(((null: Transactor[IO]) :: (null: Client[IO]) :: HNil).map(hoho))

  compile2[Http4sClient[IO, ?], IO]
    .compile(EitherK.rightc[Http4sClient[IO, ?], IO, Boolean](IO(true)))
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
