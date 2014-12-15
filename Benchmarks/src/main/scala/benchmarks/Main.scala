package benchmarks

import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{Options, OptionsBuilder}

object Main {

  def main(args: Array[String]): Unit = {
    for(n <- Range(args(0).toInt,args(1).toInt)) runWithThreads(n)
  }

  def runWithThreads(n: Int): Unit = {
    val opt: Options = new OptionsBuilder()
      .include(classOf[PhilosopherCompetition].getSimpleName)
      .threads(n)
      .build()

    new Runner(opt).run()
  }
}
