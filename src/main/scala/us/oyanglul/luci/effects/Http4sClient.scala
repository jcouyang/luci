package us.oyanglul.luci
package effects

import org.http4s.{EntityDecoder, Request, Response, Status}

sealed trait Http4sClient[E[_], A]
@deprecated("Please use Reader or ReaderT instead", since = "1.0")
case class Expect[E[_], A](request: E[Request[E]])(implicit d: EntityDecoder[E, A]) extends Http4sClient[E, A] {
  val decoder = d
}
@deprecated("Please use Reader or ReaderT instead", since = "1.0")
case class GetStatus[E[_]](req: E[Request[E]]) extends Http4sClient[E, Status]
@deprecated("Please use Reader or ReaderT instead", since = "1.0")
case class ExpectOr[E[_], A](request: E[Request[E]], onError: Response[E] => E[Throwable])(implicit
    d: EntityDecoder[E, A]
) extends Http4sClient[E, A] {
  val decoder = d
}
@deprecated("Please use Reader or ReaderT instead", since = "1.0")
case class Par[E[_], A, B](a: Http4sClient[E, A], b: Http4sClient[E, B]) extends Http4sClient[E, (A, B)]
