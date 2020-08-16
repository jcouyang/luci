package us.oyanglul.luci.compilers

import cats.data.{Kleisli, Validated}
import cats.{MonadError, ~>}

trait ValidatedCompiler[E[_]] {
  implicit def validatedCompiler[L](implicit M: MonadError[E, L]) =
    new Compiler[Validated[L, *], E] {
      type Env = ()
      val compile = Lambda[Validated[L, *] ~> Bin](v =>
        Kleisli(_ => {
          M.fromEither(v.toEither)
        })
      )
    }
}
