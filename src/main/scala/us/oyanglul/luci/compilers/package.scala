package us.oyanglul.luci

import cats.data.Kleisli
import cats.effect.IO
import cats.~>

package object compilers {
  implicit class NaturalTransformationOps[F[_], G[_]](nat: F ~> G) {
    def liftK[E] = Lambda[F ~> Kleisli[G, E, ?]](fa => Kleisli(_ => nat(fa)))
  }
  object io extends All[IO] {
    object stateT extends StateTCompiler[IO]
    object writerT extends WriterTCompiler[IO]
    object readerT extends ReaderTCompiler[IO]
    object either extends EitherCompiler[IO]
    object doobieDB extends DoobieCompiler[IO]
    object id extends IdCompiler[IO]
    object http4sClient extends Http4sClientCompiler[IO]
  }
}
