package us.oyanglul.luci.compilers
import cats.arrow.FunctionK
import shapeless._

trait IdCompiler[E[_]] {
  implicit val idCompiler = new Compiler[E, E] {
    type Env = HNil
    val compile = FunctionK.id[E].liftK[Env]
  }
}
