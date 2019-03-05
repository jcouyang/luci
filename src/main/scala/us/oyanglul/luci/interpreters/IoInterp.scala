package us.oyanglul.luci.interpreters
import cats.arrow.FunctionK
import cats.effect.IO

trait IoInterp {
  implicit def ioInterp = FunctionK.id[IO].liftK[Any]
}
