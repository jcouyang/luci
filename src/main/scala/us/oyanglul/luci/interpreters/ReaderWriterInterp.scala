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

trait ReaderWriterInterp {
  lazy val logger = getLogger

  implicit def runReader[E[_]: Sync, C](
      implicit L: Lens[C, FunctorTell[E, Chain[E[Unit]]]]) =
    new Interpreter[E, effects.Writer, C] {
      def translate = {
        Lambda[effects.Writer ~> Kleisli[E, C, ?]](
          _ match {
            case Info(log) =>
              ReaderT { pc =>
                L.get(pc)
                  .tell(Chain.one(Sync[E].pure(logger.info(log))))
              }
            case Debug(log) => logger.debug(log).pure[Kleisli[E, C, ?]]
            case Error(log) =>
              ReaderT(
                L.get(_)
                  .tell(Chain.one(Sync[E].pure(logger.error(log)))))
          }
        )
      }
    }
}
