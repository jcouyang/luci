import scala.util.Properties._

val Http4sVersion = "0.20.0-M4"
val Specs2Version = "4.3.4"
val DoobieVersion = "0.6.0"
val CatsVersion = "1.6.0"
val MonocleVersion = "1.5.0-cats"
val LogbackVersion = "1.2.3"

scalaVersion in ThisBuild := "2.12.8"

lazy val root = (project in file("."))
  .settings(
    organization := "us.oyanglul",
    name := "luci",
    version := "0.0.1",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.typelevel"   %% "cats-free"           % CatsVersion,
      "org.tpolecat"    %% "doobie-core"         % DoobieVersion,
      "org.tpolecat"    %% "doobie-postgres"     % DoobieVersion,
      "org.tpolecat"    %% "doobie-hikari"       % DoobieVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "com.olegpy" %% "meow-mtl" % "0.2.0",
      "com.github.julien-truffaut" %% "monocle-core" % MonocleVersion,
      "com.github.julien-truffaut" %% "monocle-macro" % MonocleVersion,
      "org.specs2"      %% "specs2-core"         % Specs2Version % Test,
      "org.specs2"      %% "specs2-scalacheck"   % Specs2Version % Test,
      "org.tpolecat"    %% "doobie-specs2"       % DoobieVersion % Test,
      "org.scalamock"   %% "scalamock" % "4.1.0" % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.0" % Test
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.0-M4")
  )

scalafmtOnCompile in ThisBuild := true
