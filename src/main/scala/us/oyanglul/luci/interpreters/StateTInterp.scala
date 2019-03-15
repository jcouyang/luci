package us.oyanglul.luci
package interpreters

import cats.mtl.MonadState
import cats.{Monad}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.~>
import cats.data._
import shapeless._

trait StateTEnv[E[_], F] {
  val stateT: MonadState[E, F]
}

trait StateTInterp {
  implicit def stateTInterp[E[_]: Monad, L] =
    Lambda[StateT[E, L, ?] ~> Kleisli[E, StateTEnv[E, L], ?]](state =>
      ReaderT(env =>
        for {
          currentState <- env.stateT.get
          (nextState, value) <- state.run(currentState)
          _ <- env.stateT.set(nextState)
        } yield value))
}

trait StateTCompiler[E[_]] {
  implicit def stateTCompiler[L](implicit ev: Monad[E]) =
    new Compiler[StateT[E, L, ?], E] {
      type Env = MonadState[E, L] :: HNil
      val compile = Lambda[StateT[E, L, ?] ~> Kleisli[E, Env, ?]](state =>
        ReaderT(env =>
          for {
            currentState <- env.head.get
            (nextState, value) <- state.run(currentState)
            _ <- env.head.set(nextState)
          } yield value))
    }

}
