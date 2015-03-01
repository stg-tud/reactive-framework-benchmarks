package benchmarks

import org.openjdk.jmh.annotations.{State, Param, Scope}
import rescala.turns.{Engine, Engines, Turn}

@State(Scope.Benchmark)
class EngineParam {
  @Param(Array("synchron", "spinning", "stm", "spinningWait"))
  var engineName: String = _

  def engine: Engine[Turn] = Engines.byName(engineName)
}
