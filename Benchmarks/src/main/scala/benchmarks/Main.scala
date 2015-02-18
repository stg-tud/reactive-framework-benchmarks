package benchmarks

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

import benchmarks.philosophers.PhilosopherCompetition
import org.openjdk.jmh.results.{BenchmarkResult, IterationResult, Result, RunResult}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, TimeValue}

import scala.collection.JavaConverters.{asJavaIterableConverter, collectionAsScalaIterableConverter}

object Main {

  // so, i know that one somehow can run JMH from the commandline, and most of that stuff i do here would be taken care
  // by that. but i do not know how and this was faster than finding out.
  // for the unlikely case that case that anyone ever reads this, you might want to improve on that :)
  def main(args: Array[String]): Unit = {
    val parameters: Map[String, String] = args.map(_.split('=')).map(e => e(0) -> e(1)).toMap

    val runName = parameters("name")

    write(runName + ".aggregate", "threads, engineName, philosophers, work, score, error, unit, layout\n")
    write(runName + ".raw", "threads, engineName, philosophers, work, score, unit, layout\n")

    for (n <- parameters("threads").split(','); m <- parameters("forks").split(',')) runWithThreads(n.toInt, m.toInt, parameters)
  }

  def runWithThreads(n: Int, m: Int, parameters: Map[String, String]): Unit = {
    val optBuilder = new OptionsBuilder()
      .include(classOf[PhilosopherCompetition].getSimpleName)
      .threads(n)
      .forks(m)
      //.addProfiler(classOf[StackProfiler])
      .jvmArgsAppend("-Djmh.stack.lines=5")
      .syncIterations(false)
    parameters.filterKeys(Set("engineName", "philosophers", "work", "layout", "tableType")).foreach { case (k, v) => optBuilder.param(k, v.split(','): _*) }
    parameters.get("warmupIterations").foreach(wi => optBuilder.warmupIterations(wi.toInt))
    parameters.get("warmupTime").foreach(wi => optBuilder.warmupTime(TimeValue.milliseconds(wi.toLong)))
    parameters.get("iterations").foreach(wi => optBuilder.measurementIterations(wi.toInt))
    parameters.get("time").foreach(wi => optBuilder.measurementTime(TimeValue.milliseconds(wi.toLong)))
    val opt = optBuilder.build()

    val runResult = new Runner(opt).run()
    val rawData = runResult.asScala.flatMap { rr: RunResult =>
      rr.getBenchmarkResults.asScala.flatMap { br: BenchmarkResult =>
        br.getIterationResults.asScala.flatMap { ir: IterationResult =>
          ir.getRawPrimaryResults.asScala.map { r: Result[_] =>
            List[Any](n,
              br.getParams.getParam("engineName"),
              br.getParams.getParam("philosophers"),
              br.getParams.getParam("work"),
              r.getScore,
              r.getScoreUnit,
              br.getParams.getParam("layout"))
          }
        }
      }
    }


    val runName = parameters("name")

    append(s"$runName.raw", rawData.map(_.mkString(", ")).toList)

    val aggregateData = runResult.asScala.flatMap { rr: RunResult =>
      rr.getBenchmarkResults.asScala.map { br: BenchmarkResult =>
        List[Any](n,
          br.getParams.getParam("engineName"),
          br.getParams.getParam("philosophers"),
          br.getParams.getParam("work"),
          br.getPrimaryResult.getScore,
          br.getPrimaryResult.getScoreError,
          br.getScoreUnit,
          br.getParams.getParam("layout"))
      }
    }

    append(s"$runName.aggregate", aggregateData.map(_.mkString(", ")).toList)

  }

  def path(name: String) = Paths.get("results", s"$name.csv")


  def write(name: String, line: String): Unit = {
    Files.createDirectories(path(name).getParent)
    Files.write(path(name), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE)

  }

  def append(name: String, combinedRaw: List[String]): Unit = {
    Files.write(path(name), combinedRaw.asJava, StandardCharsets.UTF_8, StandardOpenOption.APPEND)
  }
}
