package us.oyanglul.luci
package interpreters

import cats.Monad
import cats.data.Kleisli
import cats.{~>}
import doobie.util.transactor.Transactor
import doobie.free.connection.{ConnectionIO}

trait DoobieEnv[E[_]] {
  val doobieTransactor: Transactor[E]
}

trait DoobieInterp {
  implicit def doobieInterp[E[_]: Monad]
    : ConnectionIO ~> Kleisli[E, DoobieEnv[E], ?] =
    Lambda[ConnectionIO ~> Kleisli[E, DoobieEnv[E], ?]](dbops =>
      Kleisli { _.doobieTransactor.trans.apply(dbops) })
}
