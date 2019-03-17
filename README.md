# <ruby><rb>鸕鶿</rb><rt>lu ci</rt></ruby>

Extensible Free Monad Effects

[<img src=https://upload.wikimedia.org/wikipedia/commons/0/0a/Imperial_Encyclopaedia_-_Animal_Kingdom_-_pic057_-_%E9%B8%95%E9%B6%BF%E5%9C%96.svg width=50%/>](https://en.wikisource.org/wiki/zh:%E5%8F%A4%E4%BB%8A%E5%9C%96%E6%9B%B8%E9%9B%86%E6%88%90/%E5%8D%9A%E7%89%A9%E5%BD%99%E7%B7%A8/%E7%A6%BD%E8%9F%B2%E5%85%B8/%E7%AC%AC045%E5%8D%B7)


[![Latest version](https://index.scala-lang.org/jcouyang/luci/latest.svg)](https://index.scala-lang.org/jcouyang/luci)

```
libraryDependencies += "us.oyanglul" %% "luci" % <version>"
```

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-generate-toc again -->
**Table of Contents**

- [<ruby><rb>鸕鶿</rb><rt>lu ci</rt></ruby>](#rubyrb鸕鶿rbrtlu-cirtruby)
    - [The Problem](#the-problem)
    - [The Ultimate Solution](#the-ultimate-solution)
        - [Some Effects out of the box](#some-effects-out-of-the-box)
        - [Step 1: Create DSL](#step-1-create-dsl)
        - [Step 2: Compile the Program](#step-2-compile-the-program)
        - [Step 3: Run Program](#step-3-run-program)
    - [Create Your Own Effect](#create-your-own-effect)
        - [Step 1: Create Data Type](#step-1-create-data-type)
        - [Step 2: Create Compiler](#step-2-create-compiler)
        - [Step 3 Use the effect](#step-3-use-the-effect)

<!-- markdown-toc end -->

## The Problem
When you want to mix in some native effects into your Free Monad DSL, some effects won't work

for instance, we have two effects IO and StateT, and we would like to do some math using StateT

Here is the Program
```scala
import Free.{liftInject => free}
type Program[A] = EitherK[IO, StateT[IO, Int, ?], A]
type ProgramF[A] = Free[Program, A]

def program : Program[Int] = for {
  initState <- free[Program](StateT.get[IO, Int])
  _ <- free[Program](IO(println(s"init state is $initState")))
  _ <- free[Program](StateT.modify[IO, Int](_ + 1))
  res <- free[Program](StateT.modify[IO, Int](_ + 1))
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

1. instead of using interpreter `Program ~> IO`, we can use `Program ~> Kleisli[IO, ProgramContext, ?]`, and we have a better name for it - *Compiler*
2. init state of stateful effects can then be injected into program via ProgramContext when actually running `Kleisli[IO, ProgramContext, ?]`

### Some Effects out of the box
- WriterT
- ReaderT/Kleisli
- StateT
- Either
- Http4sClient
- Doobie ConnectionIO
- Id

It's very similar but just one more step to run the Kleisli

1. create Program dsl
2. **compile** Program into a Kleisli
3. **run** Kleisli in a context


### Step 1: Create DSL

e.g. our `Program` has lot of effects... WriterT, Http4sClient, ReaderT, IO, StateT and Doobie's ConnectionIO

few of them need to be stateful across all over the program like WriterT, StateT
```scala
 type Program[A] = Eff7[
      Http4sClient[IO, ?],
      WriterT[IO, Chain[String], ?],
      ReaderT[IO, Config, ?],
      IO,
      ConnectionIO,
      StateT[IO, Int, ?],
      Either[Throwable, ?],
      A
    ]
type ProgramF[A] = Free[Program, A]
```

`EffX` is predefined alias of type to construct multiple kind in `EitherK`

Now lets start using these effects to do our work
```scala
val program = for {
    config <- free[Program](Kleisli.ask[IO, Config])
    _ <- free[Program](
    GetStatus[IO](GET(Uri.uri("https://blog.oyanglul.us"))))
    _ <- free[Program](StateT.modify[IO, Int](1 + _))
    _ <- free[Program](StateT.modify[IO, Int](1 + _))
    state <- free[Program](StateT.get[IO, Int])
    _ <- free[Program](
    WriterT.tell[IO, Chain[String]](
      Chain.one("config: " + config.token)))
    resOrError <- free[Program](sql"""select true""".query[Boolean].unique)
    _ <- free[Program](
    resOrError.handleError(e => println(s"handle db error $e")))
    _ <- free[Program](IO(println(s"im IO...state: $state")))
} yield ()
```

### Step 2: Compile the Program
if we compile our program, we should get a binary `ProgramBin`
```scala
import us.oyanglul.luci.compilers.io._
val binary = compile(program)
```
imagine that you have a binary of command line tool, when you run it you would probably need to provide some `--args`

same here, if you want to run `ProgramBin`, which is basically just a Kleisli, we need to provide args with is `ProgramContext`

### Step 3: Run Program

run the program with real `--args`
```scala
val args = (httpclient ::
    logRef.tellInstance ::
    config ::
    Unit ::
    transactor ::
    stateRef.stateInstance ::
    Unit ::
    HNil).map(coflatten)

binary.run(args)
```

for stateful `WriterT` and `StateT` here, we can get `FunctorTell` and `MonadState` instances from `Ref[IO, ?]`
and inject them into program via `ProgramContext`

each one corespond to program's effect's context

1. binary for `Http4sClient[IO, ?]` needs `Client[IO]` to run
2. binary for `WriterT[IO, Chain[String], ?]` needs `FuntorTell[IO, Chain[String]]`, presented by meow-mtl `.tellInstance`
3. binary for `ReaderT[IO, Config, ?]` needs `Config` to run
4. binary for `IO` needs nothing so `Unit`
5. binary for `ConnectionIO` needs `Transactor[IO]`
6. binary for `StateT[IO, Int, ?]` needs `MonadState[IO, Int]` to run, which presented here by meow-mtl from `.stateInstance`
7. binary for `Either[Throwable, ?]` needs nothing so `Unit`

## Create Your Own Effect

creating an new compilable effect is pretty simple in 2 steps

### Step 1: Create Data Type
This is nothing different from creating a effect data type for Free Monad

For instance we need a `s3 putObject` Effect

```scala

import com.amazonaws.services.s3.model.PutObjectResult

sealed trait S3[A]

case class PutObject(bucketName: String, fileName: String, content: String)
    extends S3[PutObjectResult]
```

### Step 2: Create Compiler

To create a compiler for new data type s3, we'll need to create instance for type class Compiler
```scala
trait Compiler[F[_], E[_]] {
  type Env <: HList
  val compile: F ~> Kleisli[E, Env, ?]
}
```

We need a type of `Env` where the program needs to be compile. e.g. S3 need a AWS S3 Client

```scala
trait S3Compiler[E[_]] {
  implicit def s3Compiler(implicit F: Applicative[E]) = new Compiler[S3, E] {
    type Env = AmazonS3 :: HNil
    val compile = Lambda[S3 ~> Kleisli[E, Env, ?]] (_ match {
      case PutObject(bucketName, fileName, content) =>
        Kleisli(env => F.pure(env.head.putObject(bucketName, fileName, content)))
    })
  }
}
```

There were few point to be noted here:

1. be mindful that the `Env` needs to be a shapeless `HList`, thus, here it's `AmazonS3 :: HNil` not `AmazonS3`
2. the compiler is generic on `E[_]`, but with restriction that E has to have instance for `Applicative` so we can use `.pure`
3. Kleisli will get env of type `AmazonS3 :: HNil`, it's just like list but more acurate on it's content, you can get `head` safely since we know that there must be an item of type `AmazonS3` in head.

### Step 3 Use the effect

to be honest you don't need to make S3Compiler so generic since you may be the only person who using it. But it's a good practic to make every thing as genric as possible.

any way to use the generic effect, you can create a specific object just for IO(or Task of your choice)
```scala
object s3IoCompiler extends S3Compiler[IO]
```

and then import it to where you need to compile
```scala
import s3IoCompiler._
```

Or, simply extends it on the object or class you intent to compile your program
```scala
object Main extends S3Compiler[IO] with All{
  ...
  val binary = compile(program)
  ...
}
```
