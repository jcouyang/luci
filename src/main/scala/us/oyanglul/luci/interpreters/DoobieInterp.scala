package us.oyanglul.luci.interpreters
import cats.Monad
import cats.data.Kleisli
import cats.{~>}
import doobie.util.transactor.Transactor
import doobie.free.connection.{ConnectionIO}
import monocle.Lens

trait DoobieInterp {

  implicit def dbInterp[E[_]: Monad, C](
      implicit
      L: Lens[C, Transactor[E]]) =
    new Interpreter[E, ConnectionIO, C] {
      def translate =
        Lambda[ConnectionIO ~> Kleisli[E, C, ?]](dbops =>
          Kleisli { ctx =>
            L.get(ctx).trans.apply(dbops)
        })
    }
}
