package de.tuda.stg.reactive.benchmarks

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import rescala._

trait SimpleREScala {

  var source: Var[Int] = _
  var result: Signal[Int] = _
  var input: AtomicInteger = _

  @Setup(Level.Iteration)
  def setup() = {
    input = new AtomicInteger(0)
    source = Var(input.get())
    result = source.map(1.+).map(1.+).map(1.+)
  }

  @Benchmark
  def run(bh: Blackhole) = {
    source.set(input.incrementAndGet())
    result.get
  }

}


@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
class SimpleREScalaThreadLocal extends SimpleREScala

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
class SimpleREScalaShared extends SimpleREScala
