package us.oyanglul.luci

import cats.data.Kleisli
import cats.~>

package object interpreters {
  implicit class NaturalTransformationOps[F[_], G[_]](nat: F ~> G) {
    def liftK[E] = Lambda[F ~> Kleisli[G, E, ?]](fa => Kleisli(_ => nat(fa)))
  }
  object all extends All
  object stateT extends StateTInterp
  object writerT extends WriterTInterp
  object readerT extends ReaderTInterp
  object doobieDB extends DoobieInterp
  object io extends IoInterp
  object http4SClientInterp$ extends Http4sClientInterp
}
