package us.oyanglul.luci.compilers

import cats.data.{Kleisli, Validated}
import cats.{MonadError, ~>}
import shapeless._

trait ValidatedCompiler[E[_]] {
  implicit def validatedCompiler[L](implicit M: MonadError[E, L]) =
    new Compiler[Validated[L, *], E] {
      type Env = HNil
      val compile = Lambda[Validated[L, *] ~> Bin](v =>
        Kleisli(_ => {
          M.fromEither(v.toEither)
        })
      )
    }
}
