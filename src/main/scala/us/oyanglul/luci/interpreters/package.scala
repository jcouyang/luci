package us.oyanglul.luci

import cats.data.Kleisli
import cats.~>

package object interpreters {
  // This is kind of eh … we need to interpret F into Kleisli so this is helpful
  implicit class NaturalTransformationOps[F[_], G[_]](nat: F ~> G) {
    def liftK[E] = Lambda[F ~> Kleisli[G, E, ?]](fa => Kleisli(_ => nat(fa)))
  }
  object all extends All
}
