package us.oyanglul

import cats.effect.concurrent.Ref
import cats.data._
import cats.effect.IO
import cats.free._
import cats.data.{Kleisli, OptionT}
import org.http4s.{Request, Response}
import cats._

package object luci {
  type FreeRoute[F[_], G[_]] =
    Kleisli[OptionT[F, ?], Request[F], Free[G, Response[F]]]
  def freeRoute[F[_]: Monad, G[_]](
      pf: PartialFunction[Request[F], Free[G, Response[F]]]): FreeRoute[F, G] =
    Kleisli(
      (req: Request[F]) => OptionT(implicitly[Monad[F]].pure(pf.lift(req))))
  type RefLog = Ref[IO, Chain[IO[Unit]]]
}
