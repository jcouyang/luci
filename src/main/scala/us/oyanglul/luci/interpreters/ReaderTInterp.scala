package us.oyanglul.luci.interpreters
import cats.arrow.FunctionK
import cats.data.Kleisli
import shapeless._

trait ReaderTInterp {
  implicit def readerTInterp[E[_], C] = FunctionK.id[Kleisli[E, C, ?]]
}

trait ReaderTCompiler[E[_]] {
  implicit def readerTCompiler[C <: HList] = new Compiler[Kleisli[E, C, ?], E] {
    type Env = C
    val compile = FunctionK.id[Kleisli[E, Env, ?]]
  }
}
