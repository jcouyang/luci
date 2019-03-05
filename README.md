<h1><ruby><rb>鸕鶿</rb><rt>lu ci</rt></ruby></h1>

Extensible Free Monad Effects

[<img src=https://upload.wikimedia.org/wikipedia/commons/0/0a/Imperial_Encyclopaedia_-_Animal_Kingdom_-_pic057_-_%E9%B8%95%E9%B6%BF%E5%9C%96.svg width=50%/>](https://en.wikisource.org/wiki/zh:%E5%8F%A4%E4%BB%8A%E5%9C%96%E6%9B%B8%E9%9B%86%E6%88%90/%E5%8D%9A%E7%89%A9%E5%BD%99%E7%B7%A8/%E7%A6%BD%E8%9F%B2%E5%85%B8/%E7%AC%AC045%E5%8D%B7)


[![Latest version](https://index.scala-lang.org/jcouyang/luci/latest.svg)](https://index.scala-lang.org/jcouyang/luci)

```
libraryDependencies += "us.oyanglul" %% "luci" % <version>"
```

## Rationale
Free Monad interpreter can't pass context across effect

But with [meow-mtl](https://github.com/oleg-py/meow-mtl), we can easily integrate mtl into Free Monad Effects

## Effects
- WriterT
- ReaderT/Kleisli
- StateT
- EitherT
- Http4sClient
- Doobie ConnectionIO
- IO

## Step 1: Define your `Program`'s effects

e.g. our `Program` has lot of effects... WriterT, Http4sClient, ReaderT, IO, StateT and Doobie's ConnectionIO
```scala
type Eff1[A] =
  EitherK[WriterT[IO, Chain[String], ?], Http4sClient[IO, ?], A]
type Eff2[A] =
  EitherK[ReaderT[IO, Config, ?], Eff1, A]
type Eff3[A] = EitherK[IO, Eff2, A]
type Eff4[A] = EitherK[StateT[IO, Int, ?], Eff3, A]
type Program[A] = EitherK[ConnectionIO, Eff4, A]
type ProgramF[A] = Free[Program, A]
```

```scala
val program = for {
    config <- Free.liftInject[Program](Kleisli.ask[IO, Config])
    state <- Free.liftInject[Program](StateT.get[IO, Int])
    _ <- Free.liftInject[Program](StateT.modify[IO, Int](1 + _))
    _ <- Free.liftInject[Program](
      WriterT.tell[IO, Chain[String]](
        Chain.one("config: " + config.environment)))
    _ <- Free.liftInject[Program](for {
       _ <- sql"""insert into test values (4)""".update.run
       _ <- sql"""insert into test values (5)""".update.run
      } yield ())
    _ <- Free.liftInject[Program](
      IO(println(s"im IO...state: $state")))
    res <- Free.liftInject[Program](Ok("live"))
  } yield res
```

## Step 2: Compile Program
if we compile our program, we should get a binary `ProgramBin`
```scala
type ProgramBin[A] = Kleisli[IO, ProgramContext, A]
val binary = program foldMap implicitly[Program ~> ProgramBin]
```

## Step 3: Run Program
imagine that you have a binary of command line tool, when you run it you would probably need to provide some `--args`

same here, if you want to run `ProgramBin`, which is basically just a Kleisli, we need to provide args with is `ProgramContext`

```scala
trait ProgramContext
        extends WriterTEnv[IO, Chain[String]]
        with StateTEnv[IO, Int]
        with HttpClientEnv[IO]
        with DoobieEnv[IO]
        with ProgramEnv

binary.run(new ProgramContext {
  val enviroment = "production"
  val stateT = stateRef.stateInstance
  val writerT = logRef.tellInstance
  val http4sClient = http4sBlazeClient
  val doobieTransactor = transactor
})
```


- [API Doc](https://oss.sonatype.org/service/local/repositories/releases/archive/us/oyanglul/luci_2.12/0.0.1/luci_2.12-0.0.1-javadoc.jar/!/us/oyanglul/luci/index.html)
