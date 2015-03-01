package benchmarks.dynamic

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import benchmarks.{EngineParam, Workload}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, ThreadParams}
import rescala._
import rescala.turns.{Engine, Ticket, Turn}


@State(Scope.Benchmark)
class StackState {

  var input: AtomicInteger = new AtomicInteger(0)

  var sources: Array[Var[Int]] = _
  var results: Array[Signal[Int]] = _
  var dynamics: Array[Signal[Int]] = _
  var engine: Engine[Turn] = _

  @Setup(Level.Iteration)
  def setup(params: BenchmarkParams, engine: EngineParam, work: Workload) = {
    this.engine = engine.engine
    val threads = params.getThreads
    implicit val e = this.engine
    sources = Range(0, threads).map(_ => Var(input.incrementAndGet())).toArray
    results = sources.map(_.map(1.+).map(1.+).map { work.consume(); 1.+ })
    dynamics = results.map { r =>
      Signals.dynamic(r) { t =>
        results(r(t) % threads)(t)
      }
    }

  }
}

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
class Stacks {

  @Benchmark
  def run(state: StackState, params: ThreadParams) = {
    val index = params.getThreadIndex % params.getThreadCount
    state.sources(index).set(state.input.incrementAndGet())(state.engine)
    state.dynamics(index).now(Ticket.dynamic(state.engine))
  }


}
