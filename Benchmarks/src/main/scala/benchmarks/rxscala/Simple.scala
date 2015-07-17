package benchmarks.rxscala

import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit
import rx.lang.scala.Observable

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
@State(Scope.Benchmark) class Simple {

  var source: Observable[Integer] = Observable.just(5)

  @Benchmark def mapping(): Unit = {
    source.map(_ + 1).map(_ + 1).map(_ + 1)
  }
}
