package us.oyanglul.luci
package effects

import fs2._

sealed trait Fs2[F[_], E[_], A]

case class StreamEmits[F[_], A, E[_]](a: Seq[F[A]])
    extends Fs2[F, E, Stream[E, A]]
