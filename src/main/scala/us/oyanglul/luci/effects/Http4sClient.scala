package us.oyanglul.luci
package effects

import org.http4s.{EntityDecoder, Request, Response, Status}

sealed trait Http4sClient[E[_], A]

case class Expect[E[_], A](request: E[Request[E]])(
    implicit d: EntityDecoder[E, A])
    extends Http4sClient[E, A] {
  val decoder = d
}
case class GetStatus[E[_]](req: E[Request[E]]) extends Http4sClient[E, Status]
case class ExpectOr[E[_], A](
    request: E[Request[E]],
    onError: Response[E] => E[Throwable])(implicit d: EntityDecoder[E, A])
    extends Http4sClient[E, A] {
  val decoder = d
}
