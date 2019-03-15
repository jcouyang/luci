package us.oyanglul.luci
package interpreters

import cats.Monad
import cats.data.Kleisli
import cats.{~>}
import doobie.util.transactor.Transactor
import doobie.free.connection.{ConnectionIO}
import shapeless._

trait DoobieEnv[E[_]] {
  val doobieTransactor: Transactor[E]
}

trait DoobieInterp {
  implicit def doobieInterp[E[_]: Monad]
    : ConnectionIO ~> Kleisli[E, DoobieEnv[E], ?] =
    Lambda[ConnectionIO ~> Kleisli[E, DoobieEnv[E], ?]](dbops =>
      Kleisli { _.doobieTransactor.trans.apply(dbops) })
}

trait DoobieCompiler[E[_]] {
  implicit def doobieInterp2(implicit ev: Monad[E]) =
    new Compiler[ConnectionIO, E] {
      type Env = Transactor[E] :: HNil
      val compile = new (ConnectionIO ~> Kleisli[E, Env, ?]) {
        def apply[A](dbops: ConnectionIO[A]) =
          Kleisli { _.head.trans.apply(dbops) }
      }
    }
}
