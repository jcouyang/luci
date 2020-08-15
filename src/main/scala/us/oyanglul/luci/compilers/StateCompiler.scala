package us.oyanglul.luci
package compilers

import cats.mtl.MonadState
import cats.{Monad}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.~>
import cats.data._
import shapeless._

trait StateCompiler[E[_]] {
  implicit def stateTCompiler[L](implicit ev: Monad[E]) =
    new Compiler[StateT[E, L, ?], E] {
      type Env = MonadState[E, L] :: HNil
      val compile = Lambda[StateT[E, L, ?] ~> Bin](state =>
        ReaderT(env =>
          for {
            currentState <- env.head.get
            n            <- state.run(currentState)
            _            <- env.head.set(n._1)
          } yield n._2
        )
      )
    }

  implicit def stateCompiler[L](implicit ev: Monad[E]) =
    new Compiler[State[L, ?], E] {
      type Env = MonadState[E, L] :: HNil
      val compile = Lambda[State[L, ?] ~> Bin](state =>
        ReaderT(env =>
          for {
            currentState <- env.head.get
            (nextState, value) = state.run(currentState).value
            _ <- env.head.set(nextState)
          } yield value
        )
      )
    }

}
