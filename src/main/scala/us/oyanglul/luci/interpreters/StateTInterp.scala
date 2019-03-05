package us.oyanglul.luci
package interpreters

import cats.mtl.MonadState
import cats.{Monad}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.~>
import cats.data._

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
