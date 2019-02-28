package us.oyanglul.luci
package interpreters

import cats.data.Chain
import cats.effect.Sync
import cats.mtl.FunctorTell
import effects._
import cats.~>
import cats.syntax.applicative._
import monocle.Lens
import org.log4s.{getLogger}
import cats.data._

trait WriterTInterp {
  lazy val logger = getLogger

  implicit def runReader[E[_]: Sync, C, A](
      implicit LT: Lens[C, FunctorTell[E, Chain[E[Unit]]]]) =
    new Interpreter[E, effects.WriterT, C] {
      def translate = {
        Lambda[effects.WriterT ~> Kleisli[E, C, ?]](
          _ match {
            case Info(log) =>
              ReaderT { pc =>
                LT.get(pc)
                  .tell(Chain.one(Sync[E].pure(logger.info(log))))
              }
            case Debug(log) => logger.debug(log).pure[Kleisli[E, C, ?]]
            case Error(log) =>
              ReaderT(
                LT.get(_)
                  .tell(Chain.one(Sync[E].pure(logger.error(log)))))
          }
        )
      }
    }
}
