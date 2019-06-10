package us.oyanglul.luci
package effects

sealed trait Rescue[F[_], A] {
  type Aux
}

case class Attempt[F[_], A](a: F[A]) extends Rescue[F, Either[Throwable, A]] {
  type Aux <: A
}
