import scala.util.Properties._

val Http4sVersion = "0.20.0"
val Specs2Version = "4.3.4"
val DoobieVersion = "0.6.0"
val CatsVersion = "1.6.0"
val LogbackVersion = "1.2.3"

inScope(Scope.GlobalScope)(
  List(
    organization := "us.oyanglul",
    licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/jcouyang/luci")),
    developers := List(
      Developer(
        "jcouyang",
        "Jichao Ouyang",
        "oyanglulu@gmail.com",
        url("https://github.com/jcouyang"))
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/jcouyang/luci"),
        "scm:git@github.com:jcouyang/luci.git"
      )
    ),
    pgpPublicRing := file("/home/circleci/repo/.gnupg/pubring.asc"),
    pgpSecretRing := file("/home/circleci/repo/.gnupg/secring.asc"),
    releaseEarlyWith := SonatypePublisher,
    scalaVersion := "2.12.8"
  )
)

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-client" % Http4sVersion,
      "org.typelevel"   %% "cats-free"           % CatsVersion,
      "org.tpolecat"    %% "doobie-core"         % DoobieVersion,
      "com.olegpy"      %% "meow-mtl"            % "0.2.0",
      "org.tpolecat"    %% "doobie-postgres"     % DoobieVersion % Test,
      "org.tpolecat"    %% "doobie-hikari"       % DoobieVersion % Test,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion % Test,
      "org.specs2"      %% "specs2-core"         % Specs2Version % Test,
      "org.specs2"      %% "specs2-scalacheck"   % Specs2Version % Test,
      "org.tpolecat"    %% "doobie-specs2"       % DoobieVersion % Test,
      "org.scalamock"   %% "scalamock" % "4.1.0" % Test,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.0" % Test
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.0-M4")
  )

scalafmtOnCompile in ThisBuild := true
