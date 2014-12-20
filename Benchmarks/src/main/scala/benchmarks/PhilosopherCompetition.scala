package benchmarks

import java.util.concurrent.TimeUnit

import benchmarks.PhilosopherTable.{Seating, Thinking}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, Blackhole, ThreadParams}
import rescala.turns.Engines

import scala.annotation.tailrec
import scala.util.Random

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
class PhilosopherCompetition {

  @Benchmark
  def eat(comp: Competition, params: ThreadParams, work: Workload): Unit = {
    val myBlock = comp.blocks(params.getThreadIndex % comp.blocks.length)
    val seating = myBlock(Random.nextInt(myBlock.length))
    comp.table.eatOnce(seating)
    seating.philosopher.set(Thinking)(comp.table.engine)
  }

  @Benchmark
  def reference(work: Workload): Unit = Blackhole.consumeCPU(work.work)
}

@State(Scope.Benchmark)
class Workload {
  @Param(Array("0"/*, "10000", "100000", "1000000"*/))
  var work: Long = _
}

@State(Scope.Benchmark)
class Competition {

  @Param(Array("yielding", "pessimistic", "spinningInit", "synchron", "stm"))
  var engineName: String = _

  @Param(Array("16", "32", "64", "128", "256", "512", "1024", "2048"))
  var philosophers: Int = _

  @Param(Array("block", "alternating", "random"))
  var layout: String = _


  var table: PhilosopherTable = _

  var blocks: Array[Array[Seating]] = _

  @Setup
  def setup(params: BenchmarkParams, work: Workload) = {
    table = new PhilosopherTable(philosophers, work.work)(Engines.byName(engineName))
    blocks = (layout match {
      case "block" =>
        val perThread = table.seatings.size / params.getThreads
        table.seatings.sliding(perThread, perThread)
      case "alternating" => deal(table.seatings.toList, List.fill(math.min(params.getThreads, philosophers))(Nil))
      case "random" => List(table.seatings)
    }).map(_.toArray).toArray
  }

  @TearDown(Level.Iteration)
  def cleanEating(): Unit = {
    print(s"actually eaten: ${table.eaten.get()} measured: ")
    table.eaten.set(0)
    table.seatings.foreach(_.philosopher.set(Thinking)(table.engine))
  }

  @tailrec
  final def deal[A](deck: List[A], hands: List[List[A]]): List[List[A]] = deck match {
    case Nil => hands
    case card :: rest => deal(rest, hands.tail :+ (card :: hands.head))
  }

}
