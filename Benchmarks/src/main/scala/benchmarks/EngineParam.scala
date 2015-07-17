package benchmarks

import org.openjdk.jmh.annotations.{Param, Scope, State}
import rescala.graph.Spores
import rescala.synchronization.Engines
import rescala.turns.{Engine, Turn}

@State(Scope.Benchmark)
class EngineParam {
  @Param(Array("synchron", "spinning", "stm"))
  var engineName: String = _

  @Param(Array("7"))
  var spinningBackOff: Int = _

  def engine[S <: Spores]: Engine[S, Turn[S]] = {
    if (engineName == "spinning") Engines.spinningWithBackoff(spinningBackOff).asInstanceOf[Engine[S, Turn[S]]]
    else Engines.byName(engineName)
  }
}
