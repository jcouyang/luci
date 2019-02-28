package us.oyanglul.luci.interpreters
import cats.Monad
import cats.data.Kleisli
import cats.{~>}
import doobie.free.connection.ConnectionOp
import doobie.util.transactor.Transactor
import java.sql.Connection
import monocle.Lens
import cats.syntax.flatMap._

trait DoobieInterp {

  implicit def dbInterp[E[_]: Monad, C](implicit L: Lens[C, Transactor[E]],
                                        tx: Transactor[E]) =
    new Interpreter[E, ConnectionOp, C] {
      val dbinterp: ConnectionOp ~> Kleisli[E, Connection, ?] = tx.interpret
      def translate =
        dbinterp andThen Lambda[Kleisli[E, Connection, ?] ~> Kleisli[E, C, ?]](
          dbBin =>
            Kleisli { c =>
              val tx = L.get(c)
              tx.connect(tx.kernel).flatMap(dbBin.apply(_))
          })
    }
}
