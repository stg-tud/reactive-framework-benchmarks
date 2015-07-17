name := "benchmarks"

version := "0.0.0"

scalaVersion := "2.11.6"

lazy val benchmarks = Project(
  id = "benchmarks",
  base = file("."),
  dependencies = List(rescala, sidup, scalaReact))

lazy val rescalaRoot = RootProject(file("../../REScala"))

lazy val rescala = ProjectRef(rescalaRoot.build, "rescalaJVM")

lazy val sidupRoot = RootProject(file("../../SID-UP"))

lazy val sidup = ProjectRef(sidupRoot.build, "core")

lazy val scalaReact = ProjectRef(file("../../scala-react"), "scala-react")

mainClass in Compile := Some("org.openjdk.jmh.Main")

com.typesafe.sbt.SbtStartScript.startScriptForClassesSettings

TaskKey[Unit]("compileJmh") <<= Seq(compile in pl.project13.scala.sbt.SbtJmh.JmhKeys.Jmh).dependOn

//mainClass in (Compile, run) := Some("benchmarks.Main")

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
    //"-Ywarn-value-discard" ::
    "-Ywarn-dead-code" ::
    //"-Yno-predef" ::
    //"-Yno-imports" ::
    Nil)

javaOptions ++= (
  "-server" ::
    //"-verbose:gc" ::
    "-Xms512M" ::
    "-Xmx512M" ::
    //"-XX:NewRatio=1" ::
    //"-XX:CompileThreshold=100" ::
    //"-XX:+PrintCompilation" ::
    //"-XX:+PrintGCDetails" ::
    //"-XX:+UseParallelGC" ::
    Nil)

resolvers ++= (Nil)

libraryDependencies ++= (
  "com.lihaoyi" %% "scalarx" % "0.2.8" ::
  //"de.tuda.stg" %% "rescala" % "0.3.0" ::
  //"de.tuda.stg" %% "sidup-core" % "0.1.1-STM" ::
  //"github.com.ingoem" %% "scala-react" % "1.0" ::
  "org.scala-stm" %% "scala-stm" % "0.7" ::
  Nil)

initialCommands in console := """
                              """
