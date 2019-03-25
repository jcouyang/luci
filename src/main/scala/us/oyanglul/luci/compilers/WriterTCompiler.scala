package us.oyanglul.luci
package compilers

import cats.{Monad, Applicative}
import cats.syntax.flatMap._
import cats.data.{WriterT}
import cats.kernel.Semigroup
import cats.mtl.FunctorTell
import cats.~>
import cats.data._
import cats.syntax.apply._
import shapeless._

trait WriterTCompiler[E[_]] {
  implicit def writerTCompiler[L: Semigroup](implicit ev: Monad[E]) =
    new Compiler[WriterT[E, L, ?], E] {
      type Env = FunctorTell[E, L] :: HNil
      val compile = Lambda[WriterT[E, L, ?] ~> Bin](writer =>
        ReaderT(env => {
          writer.run.flatMap {
            case (l, v) =>
              env.head.tell(l) *>
                Monad[E].pure(v)
          }
        }))
    }

  implicit def writerCompiler[L: Semigroup](implicit ev: Applicative[E]) =
    new Compiler[Writer[L, ?], E] {
      type Env = FunctorTell[E, L] :: HNil
      val compile = Lambda[Writer[L, ?] ~> Bin](writer =>
        ReaderT(env => {
          val (l, v) = writer.run
          env.head.tell(l) *> Applicative[E].pure(v)
        }))
    }
}
