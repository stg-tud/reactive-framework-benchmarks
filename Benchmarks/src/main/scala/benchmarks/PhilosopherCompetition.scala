package benchmarks

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._
import rescala.turns.Engines

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
class PhilosopherCompetition {
  
  @Benchmark
  def goooooo(pt: Table): Unit = {
    pt.pt.run(4, 10000)
  }
}


@State(Scope.Benchmark)
class Table {

  val pt = new PhilosopherTable(32)(Engines.synchron)

//  @Setup(Level.Trial)
//  def setup() = {
//    source = RI.makeSignal(input.get())
//    result = source.map(1.+).map(1.+).map(1.+)
//
//  }

}
