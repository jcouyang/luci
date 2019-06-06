package us.oyanglul.luci
package compilers

import cats.data.Kleisli
import cats.{MonadError, ~>}
import effects._

trait RescueCompiler[E[_]] {
  type Out[FF[_], A] = FF[Either[Throwable, A]]
  implicit def rescueCompiler[F[_]](implicit M: MonadError[E, Throwable],
                                    _compiler: Compiler[F, E]) = {
    new Compiler[Rescue[Out[F, ?], ?], Out[E, ?]] {
      type Env = _compiler.Env
      val compile = new (Rescue[Out[F, ?], ?] ~> Bin) {
        def apply[A](f: Rescue[Out[F, ?], A]): Kleisli[Out[E, ?], Env, A] = {
          f match {
            case Attempt(a) =>
              _compiler.compile(a).mapF[Out[E, ?], A] { io =>
                M.attempt(io)
              }
          }
        }
      }
    }
  }

}
