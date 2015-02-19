package benchmarks.grid

import java.util.concurrent.atomic.AtomicInteger

import benchmarks.GetRI
import interface.ReactiveInterface
import org.openjdk.jmh.annotations.{Param, Scope, State, Level, Setup}

@State(Scope.Benchmark)
class SomeState {

  @Param(Array("REScala", "SIDUP", "scala.react", "scala.rx"))
  var riname: String = _
  
  lazy val RI: ReactiveInterface = GetRI(riname)

  @Param(Array("16"))
  var sources: Int = _

  @Param(Array("16"))
  var depth: Int = _


  import RI.SignalOps

  var grid: Grid = _

  @Setup(Level.Trial)
  def setup() = {
    grid = new Grid(RI, Pos(sources, depth), p => {
      val pred = p.copy(y = p.y - 1)
      pred :: (if (p.x % p.y == 0) List(p.copy(x = p.x - p.y)) else Nil)
    })
  }
}

class Bench {

}
