package us.oyanglul.luci.compilers
import cats.arrow.FunctionK
import cats.data.Kleisli
import shapeless._

trait ReaderTCompiler[E[_]] {
  implicit def readerTCompiler[C <: HList] = new Compiler[Kleisli[E, C, ?], E] {
    type Env = C
    val compile = FunctionK.id[Kleisli[E, Env, ?]]
  }
}
