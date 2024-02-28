import mill._, scalalib._, scalajslib._

trait AppScalaModule extends ScalaModule {
  def scalaVersion = "3.3.1"

  val http4sVersion     = "0.23.25"
  val catsEffectVersion = "3.3.14"
  val circeVersion      = "0.14.0"

  def http4sDeps = Agg(
    ivy"org.http4s::http4s-ember-server:$http4sVersion",
    ivy"org.http4s::http4s-dsl:$http4sVersion",
    ivy"org.http4s::http4s-circe:$http4sVersion"
  )

  def catsEffectDeps = Agg(
    ivy"org.typelevel::cats-effect:$catsEffectVersion"
  )

  def testDeps = Agg(
    ivy"com.lihaoyi::utest:0.8.2"
  )

  def circeDeps = Agg(
    ivy"io.circe::circe-generic:$circeVersion",
    ivy"io.circe::circe-parser:$circeVersion",
    ivy"io.circe::circe-fs2:$circeVersion"
  )

  def fs2KafkaDeps = Agg(
    ivy"com.github.fd4s::fs2-kafka:3.2.0"
  )

  def pureconfigDeps = Agg(
    ivy"com.github.pureconfig::pureconfig-core:0.17.5"
  )

  def loggerDeps = Agg(
    ivy"org.typelevel::log4cats-slf4j:2.6.0"
  )

  def scalacOptions = Seq(
    "-Wunused:all"
  )
}

trait AppScalaJSModule extends AppScalaModule with ScalaJSModule {
  def scalaJSVersion = "1.15.0"
}

object consumer extends Module with AppScalaModule {
  def moduleDeps = Seq(shared.jvm)

  val doobieVersion   = "1.0.0-RC4"
  val log4catsVersion = "2.4.0"
  val slf4jVersion    = "2.0.0"

  def ivyDeps = Agg(
    ivy"io.circe::circe-generic:$circeVersion",
    ivy"io.circe::circe-fs2:$circeVersion",
    ivy"co.fs2::fs2-core:3.9.4",
    ivy"co.fs2::fs2-io:3.9.4",
    ivy"org.tpolecat::doobie-core:$doobieVersion",
    ivy"org.tpolecat::doobie-hikari:$doobieVersion",
    ivy"org.tpolecat::doobie-postgres:$doobieVersion",
    ivy"org.typelevel::log4cats-slf4j:$log4catsVersion",
    ivy"org.slf4j:slf4j-api:2.0.12",
    ivy"org.slf4j:slf4j-simple:2.0.12"
  ) ++
    http4sDeps ++
    pureconfigDeps ++
    catsEffectDeps ++
    fs2KafkaDeps

  object test extends ScalaTests {
    def ivyDeps       = Agg(ivy"com.lihaoyi::utest:0.7.11")
    def testFramework = "utest.runner.Framework"
  }
}

object web_client extends ScalaJSModule {
  def scalaVersion   = "3.3.1"
  def scalaJSVersion = "1.15.0"
  def moduleDeps     = Seq(shared.js)

  def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::2.4.0",
    ivy"io.indigoengine::tyrian-io::0.10.0"
  )

  override def moduleKind = T(mill.scalajslib.api.ModuleKind.CommonJSModule)
}

object producer extends Module with AppScalaModule {
  def moduleDeps = Seq(shared.jvm)
  def ivyDeps = Agg(
    ivy"org.http4s::http4s-ember-client:$http4sVersion",
    ivy"org.slf4j:slf4j-api:2.0.12",
    ivy"org.slf4j:slf4j-simple:2.0.12"
  ) ++
    http4sDeps ++
    catsEffectDeps ++
    circeDeps ++
    fs2KafkaDeps ++
    pureconfigDeps ++
    loggerDeps

  object test extends ScalaTests {
    def ivyDeps       = testDeps
    def testFramework = "utest.runner.Framework"
  }
}

object shared extends Module {
  trait SharedModule extends AppScalaModule with PlatformScalaModule {}

  object jvm extends SharedModule {
    def ivyDeps = http4sDeps ++ circeDeps
  }
  object js extends SharedModule with AppScalaJSModule {
    def ivyDeps = Agg(
      ivy"org.http4s::http4s-dsl::$http4sVersion",
      ivy"org.http4s::http4s-circe::$http4sVersion",
      ivy"io.circe::circe-generic::$circeVersion",
      ivy"io.circe::circe-parser::$circeVersion"
    )
  }
}
