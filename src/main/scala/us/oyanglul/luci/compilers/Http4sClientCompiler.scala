package us.oyanglul.luci
package compilers
import cats.data.{Kleisli}
import effects._
import cats._
import org.http4s.client.{Client}
import shapeless._
import cats.MonadError
import cats.syntax.applicativeError._
import org.http4s.Status

trait Http4sClientCompiler[E[_]] {
  implicit def http4sClientCompiler(implicit M: MonadError[E, Throwable]) =
    new Compiler[Http4sClient[E, ?], E] {
      type Env = Client[E] :: HNil
      val compile = new (Http4sClient[E, ?] ~> Bin) {
        def apply[A](a: Http4sClient[E, A]) = {
          a match {
            case client @ Expect(request) =>
              implicit val d = client.decoder
              Kleisli(_.head.expect[A](request))
            case client @ ExpectOr(request, onError) => {
              implicit val d = client.decoder
              Kleisli(_.head.expectOr[A](request)(onError))
            }
            case client: GetStatus[E] =>
              Kleisli((env: Env) => env.head.status(client.req))
                .handleError(_ => Status.ServiceUnavailable)
          }
        }
      }
    }
}
