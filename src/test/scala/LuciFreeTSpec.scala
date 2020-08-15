package us.oyanglul.luci

import cats.data._
import resources._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext
import cats.free.FreeT.{liftInject => free}
import cats.syntax.all._
import cats.free.FreeT
import cats.effect.IO
import cats.effect.concurrent.Ref
import com.olegpy.meow.effects._
import shapeless.HNil
import compilers.io._
import compilers.coflatten

class LuciFreeTSpec extends Specification with DatabaseResource {
  implicit val cs = IO.contextShift(ExecutionContext.global)
  type Program[A] = Eff3[
    Writer[Chain[String], ?],
    IO,
    State[Int, ?],
    A
  ]

  type ProgramF[A] = FreeT[Program, IO, A]

  val program: ProgramF[Unit] = for {
    _ <- free[IO, Program](State.modify[Int](1 + _))
    _ <- free[IO, Program](State.modify[Int](1 + _))
    e <- free[IO, Program](IO.raiseError[String](new Exception("should be catch")))
      .handleError(e => s"catch $e")
    state <- free[IO, Program](State.get[Int])
    _     <- free[IO, Program](Writer.tell[Chain[String]](Chain.one("lalala")))
    _     <- free[IO, Program](IO(println(s"im IO...$e...state: $state")))
  } yield ()

  val stateRuntime = Ref[IO].of(1).unsafeRunSync().stateInstance
  val writerRuntime =
    Ref.of[IO, Chain[String]](Chain.empty).unsafeRunSync().tellInstance

  val runtime = writerRuntime :: () :: stateRuntime :: HNil
  val bin     = compile(program)
  "run FreeT" in {
    bin.run(runtime.map(coflatten)).unsafeRunSync() must_== ()
  }

}
