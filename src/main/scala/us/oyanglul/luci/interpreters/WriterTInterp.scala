package us.oyanglul.luci
package interpreters

import cats.data.Chain
import cats.effect.Sync
import cats.mtl.FunctorTell
import effects._
import cats.~>
import org.log4s.{getLogger}
import cats.data._

trait WriterTTeller[E[_]] {
  val teller: FunctorTell[E, Chain[E[Unit]]]
}

trait WriterTInterp {
  lazy val logger = getLogger

  implicit def writerInterp[E[_]: Sync, C, A] =
    Lambda[effects.WriterT ~> Kleisli[E, WriterTTeller[E], ?]](
      _ match {
        case Info(log) =>
          ReaderT { _.teller.tell(Chain.one(Sync[E].pure(logger.info(log)))) }
        case Error(log) =>
          ReaderT(_.teller.tell(Chain.one(Sync[E].pure(logger.error(log)))))
      }
    )
}
