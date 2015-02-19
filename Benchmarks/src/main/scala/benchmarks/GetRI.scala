package benchmarks

import interface.ReactiveInterface


object GetRI {
  def apply(name: String): ReactiveInterface = name match {
    case "REScala" => interface.ReactiveInterface.rescalaInstance
    case "SIDUP" => interface.ReactiveInterface.sidup
    case "scala.react" => ReactiveInterface.scalaReact()
    case "scala.rx" => ReactiveInterface.scalaRx
  }
}
