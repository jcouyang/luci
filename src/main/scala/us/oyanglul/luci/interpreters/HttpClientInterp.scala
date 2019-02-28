package us.oyanglul.luci
package interpreters
import cats.data.{Kleisli}
import effects._
import cats._
import monocle.Lens
import org.http4s.client.{Client}

trait HttpClientInterp {
  implicit def runHttp[E[_], C: Lens[?, Client[E]]]
    : Interpreter[E, HttpClient[E, ?], C] =
    new Interpreter[E, HttpClient[E, ?], C] {
      override def translate = new (HttpClient[E, ?] ~> Kleisli[E, C, ?]) {
        def apply[A](a: HttpClient[E, A]) = {
          a match {
            case b @ Expect(request) =>
              implicit val d = b.decoder
              Kleisli(ctx => {
                val httpClient: Client[E] =
                  implicitly[Lens[C, Client[E]]].get(ctx)
                httpClient.expect[A](request)
              })
            case c: GetStatus[E] =>
              Kleisli(ctx => {
                val httpClient: Client[E] =
                  implicitly[Lens[C, Client[E]]].get(ctx)
                httpClient.status(c.req)
              })
          }
        }
      }
    }
}
