package us.oyanglul.luci.interpreters
import cats.Monad
import cats.data.Kleisli
import cats.{~>}
import doobie.util.transactor.Transactor
import doobie.free.connection.{ConnectionIO}

trait DoobieTransactor[E[_]] {
  val transactor: Transactor[E]
}

trait DoobieInterp {
  implicit def dbInterp[E[_]: Monad] =
    Lambda[ConnectionIO ~> Kleisli[E, DoobieTransactor[E], ?]](dbops =>
      Kleisli { _.transactor.trans.apply(dbops) })
}
