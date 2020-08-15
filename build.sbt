import scala.util.Properties._
val dotty = "0.25.0"
val scala213 = "2.13.3"
val scala212 = "2.12.12"
lazy val supportedScalaVersions = List(dotty, scala213, scala212)

val Http4sVersion = "0.21.7"
val Specs2Version = "4.10.3"
val DoobieVersion = "0.9.0"
val CatsVersion = "2.2.0-RC2"
val LogbackVersion = "1.2.3"

inScope(Scope.GlobalScope)(
  List(
    organization := "us.oyanglul",
    licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT")),
    developers := List(
      Developer(
        "jcouyang",
        "Jichao Ouyang",
        "oyanglulu@gmail.com",
        url("https://github.com/jcouyang"))
    ),
    pgpPublicRing := file("/home/circleci/repo/.gnupg/pubring.asc"),
    pgpSecretRing := file("/home/circleci/repo/.gnupg/secring.asc"),
    releaseEarlyWith := SonatypePublisher,
    scalaVersion := scala213
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "Luci",
    crossScalaVersions := supportedScalaVersions,
    homepage := Some(url("https://github.com/jcouyang/luci")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/jcouyang/luci"),
        "scm:git@github.com:jcouyang/luci.git"
      )
    ),
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-client" % Http4sVersion,
      "org.http4s"      %% "http4s-core" % Http4sVersion,
      "org.http4s"      %% "http4s-dsl" % Http4sVersion,
      "org.typelevel"   %% "cats-free"           % CatsVersion,
      "org.tpolecat"    %% "doobie-core"         % DoobieVersion,
      "com.olegpy"      %% "meow-mtl"            % "0.3.0-M1",
      "org.tpolecat"    %% "doobie-postgres"     % DoobieVersion % Test,
      "org.tpolecat"    %% "doobie-hikari"       % DoobieVersion % Test,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion % Test,
      "org.specs2"      %% "specs2-core"         % Specs2Version % Test,
      "org.specs2"      %% "specs2-scalacheck"   % Specs2Version % Test,
      "org.tpolecat"    %% "doobie-specs2"       % DoobieVersion % Test,
      "org.scalamock"   %% "scalamock" % "5.0.0" % Test,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3" % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3")
  )

scalafmtOnCompile in ThisBuild := true
