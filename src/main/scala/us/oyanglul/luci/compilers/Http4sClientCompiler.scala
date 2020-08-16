package us.oyanglul.luci
package compilers
import cats.data.Kleisli
import effects.{Http4sClient, Expect, ExpectOr, GetStatus, Par}
import cats._
import org.http4s.client.Client
import cats.MonadError
import cats.syntax.applicativeError._
import cats.syntax.parallel._
import cats.syntax.functor._
import org.http4s._
import org.http4s.dsl.io._

case class Http4sClientError(message: String, cause: Option[Throwable] = None) extends MessageFailure {
  def toHttpResponse[F[_]](httpVersion: HttpVersion): Response[F] =
    Response(ServiceUnavailable, httpVersion)
}

trait Http4sClientCompiler[E[_]] {
  implicit def http4sClientCompiler[G[_]](implicit M: MonadError[E, Throwable], P: Parallel[E]) =
    new Compiler[Http4sClient[E, ?], E] {
      type Env = Client[E]
      val compile = new (Http4sClient[E, ?] ~> Bin) {
        def apply[A](a: Http4sClient[E, A]) = {
          a match {
            case client @ Expect(request) =>
              implicit val d: EntityDecoder[E, A] = client.decoder
              Kleisli((env: Env) =>
                env.expectOr[A](request)(error =>
                  request.map(r =>
                    new Http4sClientError(s"Http4Client ERROR response ${error} when sending request $r")
                  )
                )
              )
            case client @ ExpectOr(request, onError) => {
              implicit val d: EntityDecoder[E, A] = client.decoder
              Kleisli((env: Env) => env.expectOr[A](request)(onError))
            }
            case client: GetStatus[E] =>
              Kleisli((env: Env) => env.status(client.req))
                .handleError(_ => Status.ServiceUnavailable)
            case Par(a, b) =>
              (this.apply(a), this.apply(b)).parTupled
          }
        }
      }
    }
}
