package benchmarks

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import interface.ReactiveInterface
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.{OptionsBuilder, Options}
import rescala.signals._




object GetRI {
  def apply(name: String): ReactiveInterface = name match {
    case "REScala" => interface.ReactiveInterface.rescalaInstance
    case "SIDUP" => interface.ReactiveInterface.sidup
    case "scala.react" => ReactiveInterface.scalaReact()
    case "scala.rx" => ReactiveInterface.scalaRx
  }
}
