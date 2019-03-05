package us.oyanglul.luci
package interpreters
import cats.data.{Kleisli}
import effects._
import cats._
import org.http4s.client.{Client}

trait HttpClientEnv[E[_]] {
  val client: Client[E]
}
trait HttpClientInterp {
  implicit def runHttp[E[_]] =
    new (HttpClient[E, ?] ~> Kleisli[E, HttpClientEnv[E], ?]) {
      def apply[A](a: HttpClient[E, A]) = {
        a match {
          case b @ Expect(request) =>
            implicit val d = b.decoder
            Kleisli(_.client.expect[A](request))
          case c: GetStatus[E] =>
            Kleisli(_.client.status(c.req))
        }
      }
    }
}
