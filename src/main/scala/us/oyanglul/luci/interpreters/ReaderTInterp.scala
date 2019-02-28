package us.oyanglul.luci.interpreters
import cats.arrow.FunctionK
import cats.data.ReaderT
import cats.~>

trait ReaderTInterp {

  implicit def readerTInterp[E[_], C] =
    new Interpreter[E, ReaderT[E, C, ?], C] {
      override def translate: ReaderT[E, C, ?] ~> ReaderT[E, C, ?] =
        FunctionK.id
    }
}
