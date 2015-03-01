package benchmarks

import org.openjdk.jmh.annotations.{Param, Scope, State}

@State(Scope.Benchmark)
class Workload {
  @Param(Array("0" /*, "10000", "100000", "1000000"*/))
  var work: Long = _
}
