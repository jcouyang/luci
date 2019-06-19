package us.oyanglul.luci
package compilers

import effects._
import cats.data.Kleisli
import cats.{~>}
import fs2._
import shapeless._
import cats.effect._

trait Fs2Compiler[E[_]] {

  implicit def fs2Compiler[F[_]](implicit ev1: Concurrent[E],
                                 _compiler: Compiler[F, E]) = {
    new Compiler[Fs2[F, E, ?], E] {
      type Env = (Int :: _compiler.Env) :: HNil
      val compile = new (Fs2[F, E, ?] ~> Bin) {
        def apply[A](stream: Fs2[F, E, A]): Bin[A] =
          stream match {
            case StreamEmits(xs) =>
              Kleisli { (env: Env) =>
                Sync[E].delay {
                  Stream
                    .emits(xs)
                    .covary[E]
                    .parEvalMap(env.head.head)(alg =>
                      _compiler.compile(alg).run(env.head.tail))
                }
              }
          }
      }
    }

  }

}
