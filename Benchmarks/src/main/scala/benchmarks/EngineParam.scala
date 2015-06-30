package benchmarks

import org.openjdk.jmh.annotations.{Param, Scope, State}
import rescala.turns.{Engine, Engines, Turn}

@State(Scope.Benchmark)
class EngineParam {
  @Param(Array("synchron", "spinning", "stm", "pipelining"))
  var engineName: String = _

  @Param(Array("7"))
  var spinningBackOff: Int = _

  def engine: Engine[Turn] = {
    if (engineName == "spinning") Engines.spinningWithBackoff(spinningBackOff)
    else Engines.byName(engineName)
  }
}
