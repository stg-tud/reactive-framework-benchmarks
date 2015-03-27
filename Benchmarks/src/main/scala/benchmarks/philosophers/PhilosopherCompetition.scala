package benchmarks.philosophers

import java.util.concurrent.TimeUnit

import benchmarks.Util.deal
import benchmarks.philosophers.PhilosopherTable.{Seating, Thinking}
import benchmarks.{EngineParam, Workload}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, ThreadParams}

import scala.util.Random

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
class PhilosopherCompetition {

  @Benchmark
  def eat(comp: Competition, params: ThreadParams, work: Workload): Unit = {
    val myBlock = comp.blocks(params.getThreadIndex % comp.blocks.length)
    while ( {
      val seating = myBlock(Random.nextInt(myBlock.length))
      val res = comp.table.tryEat(seating)
      if (res) seating.philosopher.set(Thinking)(comp.table.engine)
      !res
    }) {}

  }
}


@State(Scope.Benchmark)
class Competition {

  @Param(Array("32", "256"))
  var philosophers: Int = _

  @Param(Array("block", "alternating", "random"))
  var layout: String = _

  var table: PhilosopherTable = _

  var blocks: Array[Array[Seating]] = _

  @Setup
  def setup(params: BenchmarkParams, work: Workload, engineParam: EngineParam) = {
    table = new PhilosopherTable(philosophers, work.work)(engineParam.engine)

    blocks = (layout match {
      case "block" =>
        val perThread = table.seatings.size / params.getThreads
        table.seatings.sliding(perThread, perThread)
      case "alternating" => deal(table.seatings.toList, math.min(params.getThreads, philosophers))
      case "third" => deal(table.seatings.sliding(3, 3).map(_.head).toList, params.getThreads)
      case "random" => List(table.seatings)
    }).map(_.toArray).toArray
  }

  @TearDown(Level.Iteration)
  def cleanEating(): Unit = {
    //print(s"actually eaten: ${ table.eaten.get() } measured: ")
    table.eaten.set(0)
    table.seatings.foreach(_.philosopher.set(Thinking)(table.engine))
  }

}
