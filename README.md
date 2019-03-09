<h1><ruby><rb>鸕鶿</rb><rt>lu ci</rt></ruby></h1>

Extensible Free Monad Effects

[<img src=https://upload.wikimedia.org/wikipedia/commons/0/0a/Imperial_Encyclopaedia_-_Animal_Kingdom_-_pic057_-_%E9%B8%95%E9%B6%BF%E5%9C%96.svg width=50%/>](https://en.wikisource.org/wiki/zh:%E5%8F%A4%E4%BB%8A%E5%9C%96%E6%9B%B8%E9%9B%86%E6%88%90/%E5%8D%9A%E7%89%A9%E5%BD%99%E7%B7%A8/%E7%A6%BD%E8%9F%B2%E5%85%B8/%E7%AC%AC045%E5%8D%B7)


[![Latest version](https://index.scala-lang.org/jcouyang/luci/latest.svg)](https://index.scala-lang.org/jcouyang/luci)

```
libraryDependencies += "us.oyanglul" %% "luci" % <version>"
```

## The Problem
When you want to mix in some native effects into your Free Monad DSL, some effects won't work

for instance, we have two effects IO and StateT, and we would like to do some math using StateT

Here is the Program
```scala
type Program[A] = EitherK[IO, StateT[IO, Int, ?], A]
type ProgramF[A] = Free[Program, A]

def program : Program[Int] = for {
  initState <- liftInject[Program](StateT.get[IO, Int])
  _ <- liftInject[Program](IO(println(s"init state is $initState")))
  _ <- liftInject[Program](StateT.modify[IO, Int](_ + 1))
  res <- liftInject[Program](StateT.modify[IO, Int](_ + 1))
} yield res
```

and Interpreters
```scala
def ioInterp = FunctionK.id[IO]
def stateTInterp(initState: Int) = Lambda[StateT[IO, Int, ?] ~> IO[(Int, ?)]] { _.run(initState)}
```

If we run the program
```scala
program foldMap (ioInterp or stateTInterp(0))
```

Guess what, it doesn't work, the result will be 1 not 2

because we run the state for each effect separately in the stateTInterp

One of the option is to use [FreeT](https://typelevel.org/cats/datatypes/freemonad.html#freet)

But with FreeT:

- you can only mixin 1 effect, what if I have multiple effects that I want them to be stateful across the whole program.
- all other effects need to be lift to FreeT as well. It might have huge impact to our existing code base that is Free already.

## The Ultimate Solution

is using both [meow-mtl](https://github.com/oleg-py/meow-mtl) and ReaderT/Kleisli, then we can easily integrate mtl into Free Monad Effects

- interpreter will have type of `Program ~> Kleisli[IO, ProgramContext, ?]` instead of just `Program ~> IO`
- stateful effects can be injected into program when actually running `Kleisli[IO, ProgramContext, ?]`

### we have some Effects out of the box
- WriterT
- ReaderT/Kleisli
- StateT
- EitherT
- Http4sClient
- Doobie ConnectionIO
- IO

It's very similar but just one more step to run the kleisli

1. create Program dsl
2. compile Program into a Kleisli
3. run Kleisli in a context


### Step 1: Create DSL

e.g. our `Program` has lot of effects... WriterT, Http4sClient, ReaderT, IO, StateT and Doobie's ConnectionIO

few of them need to be stateful across all over the program like WriterT, StateT
```scala
type Program[A] = Eff6[
      Http4sClient[IO, ?],
      WriterT[IO, Chain[String], ?],
      ReaderT[IO, Config, ?],
      IO,
      ConnectionIO,
      StateT[IO, Int, ?],
      A
    ]
type ProgramF[A] = Free[Program, A]
```

`EffX` is predefined alias of type to construct multiple kind in `EitherK`

Now lets start using these effects to do our work
```scala
val program = for {
    config <- liftInject[Program](Kleisli.ask[IO, Config])
    state <- liftInject[Program](StateT.get[IO, Int])
    _ <- liftInject[Program](StateT.modify[IO, Int](1 + _))
    _ <- liftInject[Program](
      WriterT.tell[IO, Chain[String]](
        Chain.one("config: " + config.environment)))
    _ <- liftInject[Program](for {
       _ <- sql"""insert into test values (4)""".update.run
       _ <- sql"""insert into test values (5)""".update.run
      } yield ())
    _ <- liftInject[Program](
      IO(println(s"im IO...state: $state")))
    res <- liftInject[Program](Ok("live"))
  } yield res
```

### Step 2: Compile the Program
if we compile our program, we should get a binary `ProgramBin`
```scala
trait ProgramContext
	extends WriterTEnv[IO, Chain[String]]
	with StateTEnv[IO, Int]
	with HttpClientEnv[IO]
	with DoobieEnv[IO]
	with ProgramEnv

import us.oyanglul.interpreters.all._ // import all our predefined effect interpreters
import us.oyanglul.interpreters.generic._ // so we can infer the correct interpreters to use base on your `Program` type
type ProgramBin[A] = Kleisli[IO, ProgramContext, A]
val binary = program foldMap implicitly[Program ~> ProgramBin]
```
imagine that you have a binary of command line tool, when you run it you would probably need to provide some `--args`

same here, if you want to run `ProgramBin`, which is basically just a Kleisli, we need to provide args with is `ProgramContext`

### Step 3: Run Program

run the program with real `--args`
```scala
binary.run(new ProgramContext {
  val enviroment = "production"
  val stateT = stateRef.stateInstance
  val writerT = logRef.tellInstance
  val http4sClient = http4sBlazeClient
  val doobieTransactor = transactor
})
```

for stateful `WriterT` and `StateT` here, we can get `FunctorTell` and `MonadState` instances from `Ref[IO, ?]`
and inject them into program via `ProgramContext`

- `stateRef` is `Ref[IO, Int]`
- `logRef` is `Ref[IO, Chain[String]]`

## Implicit Debug
The generic interpreter is very convenient that you don't have to write interpreters like
```scala
val interpreter: Program ~> ProgramBin = writerTInterp or (stateTInterp or (http4sClientInterp or (ioInterp ...)))
```

you can simply just
```scala
import us.oyanglul.luci.interpreters.generic._
implicitly[Program ~> ProgramBin]
```

However, the problem of letting compiler to implicitly find correct interpreter for you may sometimes fail and hard to debug

in this case, you can use `us.oyanglul.luci.interpreters.debug`

```scala
import us.oyanglul.luci.interpreters.debug._

implicitly[CanInterp[WriterT[IO, Chain[String], ?], IO, ProgramContext]]
implicitly[CanInterp[ReaderT[IO, Config, ?], IO, ProgramContext]]
implicitly[CanInterp[Http4sClient[IO, ?], IO, ProgramContext]]
implicitly[CanInterp[ConnectionIO, IO, ProgramContext]]
```

to find out which effect's interpreter that the compiler has problem with
