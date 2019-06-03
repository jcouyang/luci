package us.oyanglul.luci
package compilers

import cats.Monad
import cats.free.FreeApplicative
import cats.{~>}

trait FreeApplicativeCompiler[E[_]] {
  implicit def freeApplicaitve[Prog[_]](implicit ev: Monad[E],
                                        compiler: Compiler[Prog, E]) =
    new Compiler[FreeApplicative[Prog, ?], E] {
      type Env = compiler.Env
      val compile = new (FreeApplicative[Prog, ?] ~> Bin) {
        def apply[A](progs: FreeApplicative[Prog, A]) =
          progs.foldMap(compiler.compile)
      }
    }
}
