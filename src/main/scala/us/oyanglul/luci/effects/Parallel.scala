package us.oyanglul.luci.effects

import cats.{Applicative, Traverse}

sealed trait Para[F[_], G[_], R] {
  type Value
}
object Para {
  type Aux[F[_], G[_], R, I] = Para[F, G, R] { type Value = I }
}
case class Parallel[L[_]: Traverse, G[_]: Applicative, A](value: L[G[A]]) extends Para[G, L, L[A]] { type Value = A }
