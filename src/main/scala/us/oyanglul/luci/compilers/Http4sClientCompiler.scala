package us.oyanglul.luci
package compilers
import cats.data.{Kleisli}
import effects._
import cats._
import org.http4s.client.{Client}
import shapeless._

trait Http4sClientCompiler[E[_]] {
  implicit def http4sClientCompiler =
    new Compiler[Http4sClient[E, ?], E] {
      type Env = Client[E] :: HNil
      val compile = new (Http4sClient[E, ?] ~> Bin) {
        def apply[A](a: Http4sClient[E, A]) = {
          a match {
            case b @ Expect(request) =>
              implicit val d = b.decoder
              Kleisli(_.head.expect[A](request))
            case c: GetStatus[E] =>
              Kleisli(_.head.status(c.req))
          }
        }
      }
    }
}
