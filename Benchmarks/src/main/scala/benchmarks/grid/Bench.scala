package benchmarks.grid

import java.util.concurrent.atomic.AtomicInteger

import benchmarks.Util
import interface.ReactiveInterface
import org.openjdk.jmh.annotations.{Benchmark, Mode, BenchmarkMode, Param, Scope, State, Level, Setup}
import org.openjdk.jmh.infra.{BenchmarkParams, ThreadParams}

import scala.util.Random

@State(Scope.Benchmark)
class PrimState {

  @Param(Array("REScala", "SIDUP", "scala.react", "scala.rx"))
  var riname: String = _
  
  lazy val RI: ReactiveInterface = Util.getRI(riname)

  @Param(Array("16"))
  var sources: Int = _

  @Param(Array("16"))
  var depth: Int = _


  import RI.SignalOps

  var grid: Grid = _

  var blocks: IndexedSeq[IndexedSeq[RI.IVar[String]]] = _

  @Setup(Level.Trial)
  def setup(params: BenchmarkParams) = {
    grid = new Grid(RI, Pos(sources, depth), p => {
      val pred = p.copy(y = p.y - 1)
      pred :: (if (p.x % p.y == 0) List(p.copy(x = p.x - p.y)) else Nil)
    })

    blocks = Util.deal(grid.sources.toList, math.min(params.getThreads, sources))
      .map(_.map(_.asInstanceOf[RI.IVar[String]])).map(_.toIndexedSeq).toIndexedSeq
  }
}

@BenchmarkMode(Array(Mode.Throughput))
class Bench {
  @Benchmark
  def primGrid(ps: PrimState, params: ThreadParams) = {
    val block = ps.blocks(params.getThreadIndex % ps.blocks.length)
    ps.RI.setVar(block(Random.nextInt(block.length)))(Random.nextString(5))
  }
}
