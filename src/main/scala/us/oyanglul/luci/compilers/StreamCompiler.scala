package us.oyanglul.luci
package compilers

import cats.data.Kleisli
import cats.{~>}
import fs2._
import shapeless._
import cats.effect._

trait Fs2Compiler[E[_]] {

  implicit def fs2Compiler[F[_]](implicit ev1: Concurrent[E],
                                 _compiler: Compiler[F, E]) = {
    type S[A] = Stream[E, F[A]]
    type Out[A] = Stream[E, A]
    new Compiler[S, Out] {
      type Env = Int :: _compiler.Env
      val compile = Lambda[S ~> Bin](stream =>
        Kleisli { (env: Env) =>
          stream.parEvalMap(env.head)(alg =>
            _compiler.compile(alg).run(env.tail))
      })
    }

  }

}
