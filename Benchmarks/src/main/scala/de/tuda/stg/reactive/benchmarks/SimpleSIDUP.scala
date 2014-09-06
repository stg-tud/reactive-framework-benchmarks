package de.tuda.stg.reactive.benchmarks

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import reactive.signals._

trait SimpleSIDUP {

  var source: Var[Int] = _
  var result: Signal[Int] = _
  var input: AtomicInteger = _

  @Setup(Level.Iteration)
  def setup() = {
    input = new AtomicInteger(0)
    source = Var(input.get())
    result = source.single.map(1.+).single.map(1.+).single.map(1.+)
  }

  @Benchmark
  def run(bh: Blackhole) = {
    source.<<(input.incrementAndGet())
    result.single.now
  }

}


@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
class SimpleSIDUPThreadLocal extends SimpleSIDUP

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(8)
class SimpleSIDUPShared extends SimpleSIDUP
