package us.oyanglul.luci.compilers

import cats.{Applicative, Monad, Traverse, ~>, Parallel => CP}
import cats.syntax.parallel._
import us.oyanglul.luci.effects.{Para, Parallel}

trait ParallelCompiler[E[_]] {
  implicit def parallelCompiler[F[_]: Applicative, G[_]: Traverse, L, V](implicit
      P: CP[E],
      _compiler: Compiler[F, E],
      M: Monad[E]
  ) =
    new Compiler[Para[F, G, *], E] {
      type Env = _compiler.Env
      val compile: Para[F, G, *] ~> Bin = new (Para[F, G, *] ~> Bin) {
        def apply[A](value: Para[F, G, A]): Bin[A] =
          value match {
            case Parallel(v) => v.asInstanceOf[G[F[value.Value]]].parTraverse(_compiler.compile(_))
          }
      }
    }
}
