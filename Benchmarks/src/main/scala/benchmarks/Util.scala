package benchmarks

import interface.ReactiveInterface

import scala.annotation.tailrec


object Util {
  def getRI(name: String): ReactiveInterface = name match {
    case "REScalaSpin" => interface.ReactiveInterface.rescalaInstance(rescala.turns.Engines.spinningWithBackoff(7))
    case "REScalaSpinNoWait" => interface.ReactiveInterface.rescalaInstance(rescala.turns.Engines.spinning)
    case "REScalaSpinWait" => interface.ReactiveInterface.rescalaInstance(rescala.turns.Engines.spinningWait)
    case "REScalaSTM" => interface.ReactiveInterface.rescalaInstance(rescala.turns.Engines.STM)
    case "REScalaSync" => interface.ReactiveInterface.rescalaInstance(rescala.turns.Engines.synchron)
    case "SIDUP" => interface.ReactiveInterface.sidup
    case "scala.react" => ReactiveInterface.scalaReact()
    case "scala.rx" => ReactiveInterface.scalaRx
  }


  final def deal[A](initialDeck: List[A], numberOfHands: Int): List[List[A]] = {
    @tailrec
    def loop(deck: List[A], hands: List[List[A]]): List[List[A]] =
      deck match {
        case Nil => hands
        case card :: rest => loop(rest, hands.tail :+ (card :: hands.head))
      }
    loop(initialDeck, List.fill(numberOfHands)(Nil))
  }
}
