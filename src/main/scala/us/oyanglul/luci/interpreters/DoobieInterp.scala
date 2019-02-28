package us.oyanglul.luci.interpreters
import cats.data.Kleisli
import cats.{MonadError, ~>}
import doobie.free.connection.ConnectionOp
import doobie.util.transactor.Transactor
import monocle.Lens

trait DoobieInterp {

  implicit def dbInterp[E[_], C](implicit L: Lens[C, Transactor[E]],
                                 ev: MonadError[E, Throwable]) =
    new Interpreter[E, ConnectionOp, C] {
      def translate = Lambda[ConnectionOp ~> Kleisli[E, C, ?]] { op =>
        Kleisli { ctx =>
          val tx = L.get(ctx)
          println(tx)
          val bin = tx.interpret(op)
          println(bin)
          tx.exec.apply(bin)
        }
      }
    }
}
