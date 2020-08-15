package us.oyanglul.luci
package compilers

import cats.{MonadError, ~>}
import cats.data._
import shapeless._

trait EitherCompiler[E[_]] {
  implicit def eitherCompiler[L](implicit M: MonadError[E, L]) =
    new Compiler[Either[L, ?], E] {
      type Env = HNil
      val compile = Lambda[Either[L, ?] ~> Bin](either =>
        Kleisli(_ => {
          M.fromEither(either)
        })
      )
    }

  implicit def eitherTCompiler[F[_], L](implicit M: MonadError[E, L], _compiler: Compiler[F, E]) =
    new Compiler[EitherT[F, L, ?], E] {
      type Env = _compiler.Env
      val compile = Lambda[EitherT[F, L, ?] ~> Bin](either => _compiler.compile(either.value).flatMapF(M.fromEither))
    }
}
