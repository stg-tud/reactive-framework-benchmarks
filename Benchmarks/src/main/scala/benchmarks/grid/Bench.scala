package benchmarks.grid

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import benchmarks.{RIParam, Util}
import interface.ReactiveInterface
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, ThreadParams}

import scala.util.Random

@State(Scope.Benchmark)
class PrimState {
  var riparam: RIParam = _

  lazy val RI: ReactiveInterface = riparam.RI

  @Param(Array("16"))
  var sources: Int = _

  @Param(Array("16"))
  var depth: Int = _

  import RI.SignalOps

  var grid: Grid = _

  var blocks: IndexedSeq[IndexedSeq[RI.IVar[String]]] = _

  @Setup(Level.Trial)
  def setup(params: BenchmarkParams, riparam: RIParam) = {
    this.riparam = riparam
    grid = new Grid(RI, Pos(sources, depth), Grid.prim)

    blocks = Util.deal(grid.sources.toList, math.min(params.getThreads, sources))
      .map(_.map(_.asInstanceOf[RI.IVar[String]])).map(_.toIndexedSeq).toIndexedSeq
  }
}

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(2)
class Bench {
  @Benchmark
  def primGrid(ps: PrimState, params: ThreadParams) = {
    val block = ps.blocks(params.getThreadIndex % ps.blocks.length)
    ps.RI.setVar(block(Random.nextInt(block.length)))(Random.nextString(5))
  }
}
