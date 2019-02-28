package us.oyanglul.luci.interpreters
import cats.arrow.FunctionK
import cats.effect.IO

trait IoInterp {

  implicit def ioInterp[C] = new Interpreter[IO, IO, C] {
    def translate = FunctionK.id[IO].liftK[C]
  }
}
