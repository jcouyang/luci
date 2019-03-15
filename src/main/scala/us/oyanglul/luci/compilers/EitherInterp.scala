package us.oyanglul.luci
package compilers

import cats.{MonadError, ~>}
import cats.data._
import shapeless._

trait EitherCompiler[E[_]] {
  implicit def eitherCompiler[L](implicit ev: MonadError[E, L]) =
    new Compiler[Either[L, ?], E] {
      type Env = HNil
      val compile = Lambda[Either[L, ?] ~> Kleisli[E, Env, ?]](either =>
        Kleisli(_ => {
          MonadError[E, L].fromEither(either)
        }))
    }
}
