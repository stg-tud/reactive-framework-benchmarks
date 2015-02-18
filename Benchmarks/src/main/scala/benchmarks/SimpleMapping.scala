package benchmarks

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import interface.ReactiveInterface
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, Options}
import rescala.signals._



abstract class SomeState {

  def riname: String

  lazy val RI: ReactiveInterface = GetRI(riname)

  import RI.SignalOps

  var input: AtomicInteger = new AtomicInteger(0)
  var source: RI.ISignalSource[Int] = _
  var result: RI.ISignal[Int] = _

  @Setup(Level.Iteration)
  def setup() = {
    source = RI.makeSignal(input.get())
    result = source.map(1.+).map(1.+).map(1.+)

  }
}

@State(Scope.Thread)
class LocalState extends SomeState {
  @Param(Array("REScala", "SIDUP", "scala.react", "scala.rx"))
  var riname: String = _
}

@State(Scope.Benchmark)
class SharedState extends SomeState {
  @Param(Array("REScala", "SIDUP", "scala.react", "scala.rx"))
  var riname: String = _
}


@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
class SimpleMapping {

  @Benchmark
  def local(bh: Blackhole, state: LocalState) = {
    import state._
    RI.setSignal(source)(input.incrementAndGet())
    RI.getSignal(result)
  }

  @Benchmark
  def shared(bh: Blackhole, state: SharedState) = {
    import state._
    RI.setSignal(source)(input.incrementAndGet())
    RI.getSignal(result)
  }


}
