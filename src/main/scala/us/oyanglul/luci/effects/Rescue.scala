package us.oyanglul.luci
package effects

sealed trait Rescue[F[_], A]

case class Attempt[F[_], A](a: F[A])
    extends Rescue[Lambda[a => F[Either[Throwable, a]]], A]
