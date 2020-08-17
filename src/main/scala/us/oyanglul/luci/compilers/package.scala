package us.oyanglul.luci

import cats.data.Kleisli
import cats.effect.IO
import cats.~>

package object compilers {
  implicit class NaturalTransformationOps[F[_], G[_]](nat: F ~> G) {
    def liftK[E] = Lambda[F ~> Kleisli[G, E, ?]](fa => Kleisli(_ => nat(fa)))
  }
  object io extends All[IO] {
    object state  extends StateCompiler[IO]
    object writer extends WriterCompiler[IO]
    object reader extends ReaderCompiler[IO]
    object either extends EitherCompiler[IO]
    object doobie extends DoobieCompiler[IO]
    object id     extends IdCompiler[IO]
  }
}
