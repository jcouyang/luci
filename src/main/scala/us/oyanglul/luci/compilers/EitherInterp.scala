package us.oyanglul.luci
package compilers

import cats.{MonadError}
import cats.~>
import cats.data._

trait EitherInterp {
  implicit def eitherInterp[L, E[_]: MonadError[?[_], L]] =
    Lambda[Either[L, ?] ~> Kleisli[E, Any, ?]](either =>
      Kleisli(_ => {
        MonadError[E, L].fromEither(either)
      }))
}
