package benchmarks

import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Files, Paths}

import org.openjdk.jmh.results.{Result, IterationResult, BenchmarkResult, RunResult}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{Options, OptionsBuilder}
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters.asJavaIterableConverter

object Main {

  def main(args: Array[String]): Unit = {
    for(n <- Range(args(0).toInt,args(1).toInt)) runWithThreads(n)
  }

  def runWithThreads(n: Int): Unit = {
    val opt: Options = new OptionsBuilder()
      .include(classOf[PhilosopherCompetition].getSimpleName)
      .threads(n)
      .syncIterations(false)
      .build()

    val runResult = new Runner(opt).run()
    val rawData = runResult.asScala.flatMap{ rr: RunResult =>
      rr.getBenchmarkResults.asScala.flatMap{ br: BenchmarkResult =>
        br.getIterationResults.asScala.flatMap {ir: IterationResult =>
          ir.getRawPrimaryResults.asScala.map{ r : Result[_] =>
            List[Any](n, br.getParams.getParam("engineName"), br.getParams.getParam("philosophers"), br.getParams.getParam("work"), r.getScore, r.getScoreUnit)
          }
        }
      }
    }


    val combinedRaw = "threads, engineName, philosophers, work, score, unit" :: rawData.map(_.mkString(", ")).toList

    writeResults(Paths.get("results", s"$n.raw.csv"), combinedRaw)

    val aggregateData = runResult.asScala.flatMap{ rr: RunResult =>
      rr.getBenchmarkResults.asScala.map{ br: BenchmarkResult =>
        List[Any](n, br.getParams.getParam("engineName"), br.getParams.getParam("philosophers"),
          br.getParams.getParam("work"), br.getPrimaryResult.getScore, br.getPrimaryResult.getScoreError, br.getScoreUnit)
      }
    }

    val combinedAggregate = "threads, engineName, philosophers, work, score, error, unit" :: aggregateData.map(_.mkString(", ")).toList

    writeResults(Paths.get("results", s"$n.aggregate.csv"), combinedAggregate)

  }

  def writeResults(path: Path, combinedRaw: List[String]): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, combinedRaw.asJava, StandardCharsets.UTF_8)
  }
}
