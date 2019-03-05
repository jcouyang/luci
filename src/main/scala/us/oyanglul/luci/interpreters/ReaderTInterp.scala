package us.oyanglul.luci.interpreters
import cats.arrow.FunctionK
import cats.data.ReaderT

trait ReaderTInterp {
  implicit def readerTInterp[E[_], C] = FunctionK.id[ReaderT[E, C, ?]]
}
