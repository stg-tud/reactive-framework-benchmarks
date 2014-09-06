name := "reactive-benchmarks"

version := "0.0.0"

scalaVersion := "2.11.2"

//scalaSource in Compile <<= baseDirectory {(base) => new File(base, "src")}

//scalaSource in Test <<= baseDirectory {(base) => new File(base, "test")}

//resourceDirectory in Compile <<= baseDirectory {(base) => new File(base, "resources")}

//this improves incremental compilation with sbt 13.2 and scala 2.11
//incOptions := incOptions.value.withNameHashing(true)

lazy val benchmarks = Project(
  id = "benchmarks",
  base = file("."),
  dependencies = List(rescala, sidup))

lazy val rescalaRoot = RootProject(file("../../REScala"))

lazy val rescala = ProjectRef(rescalaRoot.build, "rescala")

lazy val sidup = ProjectRef(sidupRoot.build, "core")

lazy val sidupRoot = RootProject(file("../../SID-UP"))

jmhSettings

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

libraryDependencies ++= (Nil)

initialCommands in console := """
                              """
