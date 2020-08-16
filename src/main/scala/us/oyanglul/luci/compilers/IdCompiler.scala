package us.oyanglul.luci.compilers
import cats.arrow.FunctionK

trait IdCompiler[E[_]] {
  implicit val idCompiler = new Compiler[E, E] {
    type Env = ()
    val compile = FunctionK.id[E].liftK[Env]
  }
}
