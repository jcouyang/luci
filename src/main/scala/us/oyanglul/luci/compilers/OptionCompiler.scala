package us.oyanglul.luci.compilers

import cats.data._
import cats.{MonadError, ~>}

class OptionCompilerValueEmpty extends Throwable
trait OptionCompiler[E[_]] {
  implicit def optionCompiler(implicit M: MonadError[E, Throwable]) =
    new Compiler[Option, E] {
      type Env = ()
      val compile = Lambda[Option ~> Bin](option =>
        Kleisli(_ => {
          M.fromOption(option, new OptionCompilerValueEmpty)
        })
      )
    }

  implicit def optionTCompiler[F[_]](implicit M: MonadError[E, Throwable], _compiler: Compiler[F, E]) =
    new Compiler[OptionT[F, *], E] {
      type Env = _compiler.Env
      val compile = Lambda[OptionT[F, *] ~> Bin](either =>
        _compiler.compile(either.value).flatMapF(M.fromOption(_, new OptionCompilerValueEmpty))
      )
    }
}
