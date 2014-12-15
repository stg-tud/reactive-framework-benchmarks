package benchmarks

import org.openjdk.jmh.results.format.{ResultFormatType, ResultFormat}
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.format.OutputFormatFactory
import org.openjdk.jmh.runner.options.{OptionsBuilder, Options}

object Main {

  def main(args: Array[String]): Unit = {
//    for(n <- List(1,2,4,8,12,16,20,24,28,32,36,40,44,48,52,56,60,64)) runWithThreads(n)
    runWithThreads(1)
  }

  def runWithThreads(n: Int): Unit = {
    val opt: Options = new OptionsBuilder()
      .include(classOf[PhilosopherCompetition].getSimpleName())
      .threads(n)
      .build()

    new Runner(opt).run()
    ()
  }
}
