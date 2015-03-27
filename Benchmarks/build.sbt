name := "benchmarks"

version := "0.0.0"

scalaVersion := "2.11.5"

lazy val benchmarks = Project(
  id = "benchmarks",
  base = file("."),
  dependencies = List(rescala))

lazy val rescalaRoot = RootProject(file("../../REScala"))

lazy val rescala = ProjectRef(rescalaRoot.build, "rescala")

jmhSettings

mainClass in Compile := Some("org.openjdk.jmh.Main")

com.typesafe.sbt.SbtStartScript.startScriptForClassesSettings

TaskKey[Unit]("compileJmh") <<= Seq(compile in JmhKeys.Jmh).dependOn

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