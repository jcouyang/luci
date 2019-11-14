package us.oyanglul.luci
package compilers
import cats.data.{Kleisli}
import effects._
import cats._
import org.http4s.client.{Client}
import shapeless._
import cats.MonadError
import cats.syntax.applicativeError._
import cats.syntax.applicative._
import cats.syntax.parallel._
import cats.syntax.functor._
import org.http4s._
import org.http4s.dsl.io._

case class Http4sClientError(message: String, cause: Option[Throwable] = None)
    extends MessageFailure {
  def toHttpResponse[F[_]](httpVersion: HttpVersion)(
      implicit F: Applicative[F]): F[Response[F]] =
    Response(ServiceUnavailable, httpVersion)
      .pure[F]
}
trait Http4sClientCompiler[E[_]] {
  implicit def http4sClientCompiler[G[_]](implicit M: MonadError[E, Throwable],
                                          P: Parallel[E, G]) =
    new Compiler[Http4sClient[E, ?], E] {
      type Env = Client[E] :: HNil
      val compile = new (Http4sClient[E, ?] ~> Bin) {
        def apply[A](a: Http4sClient[E, A]) = {
          a match {
            case client @ Expect(request) =>
              implicit val d = client.decoder
              Kleisli((env: Env) =>
                env.head.expectOr[A](request)(error =>
                  request.map(r =>
                    new Http4sClientError(
                      s"Http4Client ERROR response ${error} when sending request $r"))))
            case client @ ExpectOr(request, onError) => {
              implicit val d = client.decoder
              Kleisli((env: Env) => env.head.expectOr[A](request)(onError))
            }
            case client: GetStatus[E] =>
              Kleisli((env: Env) => env.head.status(client.req))
                .handleError(_ => Status.ServiceUnavailable)
            case Par(a, b) =>
              (this.apply(a), this.apply(b)).parTupled
          }
        }
      }
    }
}
