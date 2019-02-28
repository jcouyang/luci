package us.oyanglul.luci
package effects

import org.http4s.{EntityDecoder, Request, Status}

sealed trait HttpClient[E[_], A]

case class Expect[E[_], A](request: E[Request[E]])(
    implicit d: EntityDecoder[E, A])
    extends HttpClient[E, A] {
  val decoder = d
}
case class GetStatus[E[_]](req: E[Request[E]]) extends HttpClient[E, Status]
