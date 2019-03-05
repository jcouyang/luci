package us.oyanglul.luci
package interpreters
import cats.data.{Kleisli}
import effects._
import cats._
import org.http4s.client.{Client}

trait HttpClientEnv[E[_]] {
  val http4sClient: Client[E]
}
trait Http4sClientInterp {
  implicit def http4sClientInterp[E[_]] =
    new (Http4sClient[E, ?] ~> Kleisli[E, HttpClientEnv[E], ?]) {
      def apply[A](a: Http4sClient[E, A]) = {
        a match {
          case b @ Expect(request) =>
            implicit val d = b.decoder
            Kleisli(_.http4sClient.expect[A](request))
          case c: GetStatus[E] =>
            Kleisli(_.http4sClient.status(c.req))
        }
      }
    }
}
