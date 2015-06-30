package benchmarks

import interface.ReactiveInterface
import org.openjdk.jmh.annotations.{State, Param, Scope}
import rescala.turns.{Engine, Engines, Turn}

@State(Scope.Benchmark)
class RIParam {
  @Param(Array("REScalaSpin", "REScalaSTM", "REScalaSync", "REScalaPipelining", "SIDUP", "scala.react", "scala.rx"))
  var riname: String = _

  lazy val RI: ReactiveInterface = Util.getRI(riname)
}
