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

// This is a basic Mill build for a single `ScalaModule`, with two
// third-party dependencies and a test suite using the uTest framework. As a
// single-module project, it `extends RootModule` to mark `object foo` as the
// top-level module in the build. This lets us directly perform operations
// `./mill compile` or `./mill run` without needing to prefix it as
// `foo.compile` or `foo.run`.
//
// You can download this example project using the *download* link above
// if you want to try out the commands below yourself. The only requirement is
// that you have some version of the JVM installed; the `./mill` script takes
// care of any further dependencies that need to be downloaded.
//
// The source code for this module lives in the `src/` folder.
// Output for this module (compiled files, resolved dependency lists, ...)
// lives in `out/`.
//
// This example project uses two third-party dependencies - MainArgs for CLI
// argument parsing, Scalatags for HTML generation - and uses them to wrap a
// given input string in HTML templates with proper escaping.
//
// You can run `assembly` to generate a standalone executable jar, which then
// can be run from the command line or deployed to be run elsewhere.

/** Usage
  *
  * > ./mill resolve _ # List what tasks are available to run assembly ... clean ... compile ... run
  * ... show ... inspect ...
  *
  * > ./mill inspect compile # Show documentation and inputs of a task
  * compile(ScalaModule.scala:...) Compiles the current module to generate compiled
  * classfiles/bytecode. Inputs: scalaVersion upstreamCompileOutput allSourceFiles compileClasspath
  *
  * > ./mill compile # compile sources into classfiles ... compiling 1 Scala source to...
  *
  * > ./mill run # run the main method, if any error: Missing argument: --text <str>
  *
  * > ./mill run --text hello <h1>hello</h1>
  *
  * > ./mill test ... + foo.FooTests.simple ... <h1>hello</h1> + foo.FooTests.escaping ...
  * <h1>&lt;hello&gt;</h1>
  *
  * > ./mill assembly # bundle classfiles and libraries into a jar for deployment
  *
  * > ./mill show assembly # show the output of the assembly task ".../out/assembly.dest/out.jar"
  *
  * > java -jar ./out/assembly.dest/out.jar --text hello <h1>hello</h1>
  *
  * > ./out/assembly.dest/out.jar --text hello # mac/linux <h1>hello</h1>
  */

// The output of every Mill task is stored in the `out/` folder under a name
// corresponding to the task that created it. e.g. The `assembly` task puts its
// metadata output in `out/assembly.json`, and its output files in
// `out/assembly.dest`. You can also use `show` to make Mill print out the
// metadata output for a particular task.
//
// Additional Mill tasks you would likely need include:
//
// [source,bash]
// ----
// $ mill runBackground # run the main method in the background
//
// $ mill clean <task>  # delete the cached output of a task, terminate any runBackground
//
// $ mill launcher      # prepares a foo/launcher.dest/run you can run later
//
// $ mill jar           # bundle the classfiles into a jar suitable for publishing
//
// $ mill -i console    # start a Scala console within your project
//
// $ mill -i repl       # start an Ammonite Scala REPL within your project
// ----
//
// You can run `+mill resolve __+` to see a full list of the different tasks that
// are available, `+mill resolve _+` to see the tasks within `foo`,
// `mill inspect compile` to inspect a task's doc-comment documentation or what
// it depends on, or `mill show foo.scalaVersion` to show the output of any task.
//
// The most common *tasks* that Mill can run are cached *targets*, such as
// `compile`, and un-cached *commands* such as `foo.run`. Targets do not
// re-evaluate unless one of their inputs changes, whereas commands re-run every
// time.
