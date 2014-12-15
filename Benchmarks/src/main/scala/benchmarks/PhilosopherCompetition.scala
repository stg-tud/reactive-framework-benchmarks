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
  def goooooo(table: Table): Unit = {
    table.table.run(4, 10000)
  }
}


@State(Scope.Benchmark)
class Table {

  @Param(Array("pessimistic", "synchron", "unmanaged"))
  var engineName: String = _

  @Param(Array("3", "10", "15", "26", "32"))
  var philosophers: Int = _

  var table: PhilosopherTable = _

  @Setup
  def setup() = {
    table = new PhilosopherTable(philosophers)(Engines.byName(engineName))
  }

//  @Setup(Level.Trial)
//  def setup() = {
//    source = RI.makeSignal(input.get())
//    result = source.map(1.+).map(1.+).map(1.+)
//
//  }

}
