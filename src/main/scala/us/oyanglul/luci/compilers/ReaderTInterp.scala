package us.oyanglul.luci.compilers
import cats.~>
import cats.data.Kleisli
import shapeless._

trait ReaderTCompiler[E[_]] {
  implicit def readerTCompiler[C] = new Compiler[Kleisli[E, C, ?], E] {
    type Env = C :: HNil
    val compile =
      Lambda[Kleisli[E, C, ?] ~> Kleisli[E, Env, ?]](_.local(_.head))
  }
}
