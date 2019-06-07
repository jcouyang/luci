package us.oyanglul.luci
package compilers

import cats.data.Kleisli
import cats.{MonadError, ~>}
import effects._

trait RescueCompiler[E[_]] {
  implicit def rescueCompiler[F[_], B](implicit M: MonadError[E, Throwable],
                                       _compiler: Compiler[F, E]) = {
    new Compiler[Rescue[F, ?], E] {
      type Env = _compiler.Env
      val compile = new (Rescue[F, ?] ~> Bin) {
        def apply[A](f: Rescue[F, A]): Kleisli[E, Env, A] = {
          f match {
            case b @ Attempt(a) =>
              _compiler.compile(a).mapF[E, A] { (io: E[b.Aux]) =>
                M.attempt(io)
              }
          }
        }
      }
    }
  }

}
