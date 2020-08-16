package us.oyanglul.luci.compilers
import cats.arrow.FunctionK
import cats.{Applicative, Id, ~>}
import cats.data.{Kleisli, Reader}

trait ReaderCompiler[E[_]] {
  implicit def readerTCompiler[C] =
    new Compiler[Kleisli[E, C, *], E] {
      type Env = C
      val compile =
        FunctionK.id[Kleisli[E, C, *]]
    }

  implicit def readerCompiler[C](implicit F: Applicative[E]) =
    new Compiler[Reader[C, *], E] {
      type Env = C
      val compile =
        Lambda[Reader[C, *] ~> Bin](_.mapK(Lambda[Id ~> E](F.pure(_))))
    }
}
