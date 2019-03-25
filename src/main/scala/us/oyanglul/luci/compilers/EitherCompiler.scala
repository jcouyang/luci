package us.oyanglul.luci
package compilers

import cats.{MonadError, ~>}
import cats.syntax.flatMap._
import cats.data._
import shapeless._

trait EitherTCompiler[E[_]] {
  implicit def eitherCompiler[L](implicit M: MonadError[E, L]) =
    new Compiler[Either[L, ?], E] {
      type Env = HNil
      val compile = Lambda[Either[L, ?] ~> Bin](either =>
        Kleisli(_ => {
          M.fromEither(either)
        }))
    }

  implicit def eitherTCompiler[L](implicit M: MonadError[E, L]) =
    new Compiler[EitherT[E, L, ?], E] {
      type Env = HNil
      val compile = Lambda[EitherT[E, L, ?] ~> Bin](either =>
        Kleisli(_ => {
          either.value.flatMap(M.fromEither(_))
        }))
    }
}
