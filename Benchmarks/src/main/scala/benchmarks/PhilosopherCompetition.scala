package benchmarks

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, ThreadParams}
import rescala.turns.Engines
import benchmarks.PhilosopherTable.{Seating, Thinking}


import scala.annotation.tailrec
import scala.util.Random

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(1)
class PhilosopherCompetition {


  @Benchmark
  def eat(comp: Competition, params: ThreadParams): Unit = {
    val myBlock = comp.blocks(params.getThreadIndex)
    val seating = myBlock(Random.nextInt(myBlock.length))
    comp.table.eatOnce(seating)
    seating.philosopher.set(Thinking)(comp.table.engine)
  }
}


@State(Scope.Benchmark)
class Competition {

  @Param(Array("pessimistic", "synchron"))
  var engineName: String = _

  @Param(Array("3", "15", "32"))
  var philosophers: Int = _

  var table: PhilosopherTable = _

  var blocks: Array[Array[Seating]] = _

  @Setup
  def setup(params: BenchmarkParams) = {
    table = new PhilosopherTable(philosophers)(Engines.byName(engineName))
    blocks = deal(table.seatings.toList, List.fill(params.getThreads)(Nil)).map(_.toArray).toArray
  }

  @tailrec
  final def deal[A](deck: List[A], hands: List[List[A]]): List[List[A]] = deck match {
    case Nil => hands
    case card :: rest => deal(rest, hands.tail :+ (card :: hands.head))
  }

  //  @Setup(Level.Trial)
  //  def setup() = {
  //    source = RI.makeSignal(input.get())
  //    result = source.map(1.+).map(1.+).map(1.+)
  //
  //  }

}
