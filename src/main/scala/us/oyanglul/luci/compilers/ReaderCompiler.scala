package us.oyanglul.luci.compilers
import cats.{Applicative, ~>}
import cats.data.{Reader, Kleisli}
import shapeless._

trait ReaderCompiler[E[_]] {
  implicit def readerTCompiler[C] =
    new Compiler[Kleisli[E, C, ?], E] {
      type Env = C :: HNil
      val compile =
        Lambda[Kleisli[E, C, ?] ~> Bin](_.local(_.head))
    }

  implicit def readerCompiler[C](implicit F: Applicative[E]) =
    new Compiler[Reader[C, ?], E] {
      type Env = C :: HNil
      val compile =
        Lambda[Reader[C, ?] ~> Bin](_.local((e: Env) => e.head).mapK(Lambda[Id ~> E](F.pure(_))))
    }
}
