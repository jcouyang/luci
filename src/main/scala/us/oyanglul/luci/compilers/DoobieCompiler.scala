package us.oyanglul.luci
package compilers

import cats.Monad
import cats.data.Kleisli
import cats.{~>}
import doobie.util.transactor.Transactor
import doobie.free.connection.{ConnectionIO}
import shapeless._

trait DoobieCompiler[E[_]] {
  implicit def doobieInterp2(implicit ev: Monad[E]) =
    new Compiler[ConnectionIO, E] {
      type Env = Transactor[E] :: HNil
      val compile = new (ConnectionIO ~> Bin) {
        def apply[A](dbops: ConnectionIO[A]) =
          Kleisli { _.head.trans.apply(dbops) }
      }
    }
}
