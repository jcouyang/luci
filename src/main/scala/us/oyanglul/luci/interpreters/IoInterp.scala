package us.oyanglul.luci.interpreters
import cats.arrow.FunctionK
import cats.effect.IO
import shapeless._

trait IoInterp {
  implicit def ioInterp = FunctionK.id[IO].liftK[Any]
}

trait IoCompiler[E[_]] {
  implicit val ioCompiler = new Compiler[E, E] {
    type Env = HNil
    val compile = FunctionK.id[E].liftK[Env]
  }
}
