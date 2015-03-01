package benchmarks

import org.openjdk.jmh.annotations.{Param, Scope, State}
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
class Workload {
  @Param(Array("0" /*, "10000", "100000", "1000000"*/))
  var work: Long = _
  def consume() = Blackhole.consumeCPU(work)
}
