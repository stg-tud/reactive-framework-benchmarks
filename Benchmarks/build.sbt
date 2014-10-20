name := "reactive-benchmarks"

version := "0.0.0"

scalaVersion := "2.11.2"

//lazy val benchmarks = Project(
//  id = "benchmarks",
//  base = file("."),
//  dependencies = List(rescala, sidup, scalaReact))
//
//lazy val rescalaRoot = RootProject(file("../../REScala"))
//
//lazy val rescala = ProjectRef(rescalaRoot.build, "rescala")
//
//lazy val sidupRoot = RootProject(file("../../SID-UP"))
//
//lazy val sidup = ProjectRef(sidupRoot.build, "core")
//
//lazy val scalaReact = ProjectRef(file("../../scala-react"), "scala-react")



scalacOptions ++= (
  "-deprecation" ::
    "-encoding" :: "UTF-8" ::
    "-unchecked" ::
    "-feature" ::
    "-target:jvm-1.7" ::
    //"-language:implicitConversions" ::
    //"-language:reflectiveCalls" ::
    //"-language:existentials" ::
    //"-language:higherKinds" ::
    "-Xlint" ::
    "-Xfuture" ::
    //"-Xlog-implicits" ::
    "-Xfatal-warnings" ::
    "-Yno-adapted-args" ::
    "-Ywarn-numeric-widen" ::
    "-Ywarn-value-discard" ::
    "-Ywarn-dead-code" ::        // N.B. doesn't work well with the ??? hole
    //"-Yno-predef" ::   // no automatic import of Predef (removes irritating implicits)
    //"-Yno-imports" ::  // no automatic imports at all; all symbols must be imported explicitly
    Nil)

javaOptions ++= (
  "-server" ::
    //"-verbose:gc" ::
    //"-Xms512M" ::
    //"-Xmx512M" ::
    //"-XX:NewRatio=1" ::
    //"-XX:CompileThreshold=100" ::
    //"-XX:+PrintCompilation" ::
    //"-XX:+PrintGCDetails" ::
    //"-XX:+UseParallelGC" ::
    Nil)

resolvers ++= (Nil)

libraryDependencies ++= (
  "com.scalarx" %% "scalarx" % "0.2.6" ::
    "de.tuda.stg" %% "rescala" % "0.3.0" ::
    "de.tuda.stg" %% "sidup-core" % "0.1.1-STM" ::
    "github.com.ingoem" %% "scala-react" % "1.0" ::
    Nil)

initialCommands in console := """
                              """
