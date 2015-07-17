package benchmarks.pipeline

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import benchmarks.{RIParam, Util}
import interface.ReactiveInterface
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import benchmarks.Workload
import org.openjdk.jmh.infra.BenchmarkParams
import benchmarks.EngineParam
import rescala.Var
import rescala.Signal
import rescala.Signals
import rescala.turns.Engine
import rescala.turns.Turn
import org.openjdk.jmh.infra.ThreadParams


@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
class Pipeline {
  
  @Benchmark
  def updatePipeline(comp: Competition, params: ThreadParams, work: Workload): Unit = {
    comp.pipelineEngine.plan(comp.pipelineRoot){implicit turn =>
      val newValue = comp.pipelineRoot.get + 1
      comp.pipelineRoot.admit(newValue)
    }
  }
}



@State(Scope.Benchmark)
class Competition {

  @Param(Array("8", "16", "32", "64", "128", "256", "512", "1024", "2048"))
  var pipelineLength: Int = _
  var pipelineEngine : Engine[Turn] = _
  
  var pipelineRoot: Var[Int]  = _

  var reactives : List[Signal[Int]] = _

  @Setup
  def setup(params: BenchmarkParams, work: Workload, engineParam: EngineParam) = {
    implicit val engine = engineParam.engine
    pipelineEngine = engine
    pipelineRoot = Var(0)
    var currentReactive : Signal[Int] = pipelineRoot
    for (i <- 2 to pipelineLength) {
      val reactive = currentReactive
      currentReactive = Signals.static(reactive)(implicit turn => {
        work.consume()
        reactive.get + 1})
    }
  }

  @TearDown(Level.Iteration)
  def cleanEating(): Unit = {
    implicit val engine = pipelineEngine
    pipelineRoot.set(0)
  }

}