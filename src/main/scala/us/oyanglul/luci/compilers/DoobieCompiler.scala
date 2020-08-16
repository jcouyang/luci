package us.oyanglul.luci
package compilers

import cats.data.Kleisli
import cats.effect.Bracket
import cats.~>
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor

trait DoobieCompiler[E[_]] {
  implicit def doobieInterp(implicit b: Bracket[E, Throwable]) =
    new Compiler[ConnectionIO, E] {
      type Env = Transactor[E]
      val compile = new (ConnectionIO ~> Bin) {
        def apply[A](dbops: ConnectionIO[A]) =
          Kleisli { _.trans.apply(dbops) }
      }
    }
}
