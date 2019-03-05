package us.oyanglul.luci
package interpreters

import cats.{Monad}
import cats.syntax.flatMap._
import cats.data.{WriterT}
import cats.kernel.Semigroup
import cats.mtl.FunctorTell
import cats.~>
import cats.data._

trait WriterTEnv[E[_], F] {
  val writerT: FunctorTell[E, F]
}

trait WriterTInterp {
  implicit def writerInterp[E[_]: Monad, L: Semigroup] =
    Lambda[WriterT[E, L, ?] ~> Kleisli[E, WriterTEnv[E, L], ?]](writer =>
      ReaderT(env => {
        writer.run.flatMap {
          case (l, v) =>
            env.writerT.tell(l)
            Monad[E].pure(v)
        }
      }))
}
