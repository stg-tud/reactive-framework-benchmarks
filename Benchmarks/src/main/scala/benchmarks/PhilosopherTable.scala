package benchmarks

import java.util.concurrent.atomic.AtomicInteger

import org.openjdk.jmh.infra.Blackhole
import rescala.Signals.lift
import rescala.graph.Globals.named
import rescala.graph.Pulsing
import rescala.turns.{Engine, Turn}
import rescala.{Observe, Signal, Var}

import scala.annotation.tailrec
import scala.util.Random

class PhilosopherTable(philosopherCount: Int)(implicit val engine: Engine[Turn]) {

  import benchmarks.PhilosopherTable._

  val seatings = createTable(philosopherCount)

//  seatings.foreach { seating =>
//    seating.vision.observe{ state =>
//      if (state == Eating) Thread.sleep(100)
//    }
//  }


  def calcFork(leftName: String, rightName: String)(leftState: Philosopher, rightState: Philosopher): Fork =
    (leftState, rightState) match {
      case (Thinking, Thinking) => Free
      case (Hungry, _) => Taken(leftName)
      case (_, Hungry) => Taken(rightName)
    }

  def calcVision(ownName: String)(leftFork: Fork, rightFork: Fork): Vision =
    (leftFork, rightFork) match {
      case (Free, Free) => Ready
      case (Taken(`ownName`), Taken(`ownName`)) => Eating
      case (Taken(name), _) => WaitingFor(name)
      case (_, Taken(name)) => WaitingFor(name)
    }


  def createTable(tableSize: Int): Seq[Seating] = {
    def mod(n: Int): Int = (n + tableSize) % tableSize

    val phils = for (i <- 0 until tableSize) yield named(s"Phil-${ names(i) }")(Var[Philosopher](Thinking))

    val forks = for (i <- 0 until tableSize) yield {
      val nextCircularIndex = mod(i + 1)
      named(s"Fork-${ names(i) }-${ names(nextCircularIndex) }") {
        lift(phils(i), phils(nextCircularIndex))(calcFork(names(i), names(nextCircularIndex)))
      }
    }

    for (i <- 0 until tableSize) yield {
      val vision = named(s"Vision-${ names(i) }") {
        lift(forks(i), forks(mod(i - 1)))(calcVision(names(i)))
      }
      Seating(i, phils(i), forks(i), forks(mod(i - 1)), vision)
    }
  }


  def tryEat(seating: Seating): Boolean =
    engine.plan(seating.philosopher) { turn =>
      if (seating.vision(turn) == Ready) {
        seating.philosopher.admit(Hungry)(turn)
        true
      }
      else false
    } { (turn, forksWereFree) =>
      if (forksWereFree) assert(seating.vision(turn) == Eating)
      forksWereFree
    }

  def eatOnce(seating: Seating) = repeatUntilTrue(tryEat(seating))

  // ============================================== Logging =======================================================

  def log(msg: String): Unit = {
    println("[" + Thread.currentThread().getName + " @ " + System.currentTimeMillis() + "] " + msg)
  }

  def log[A](reactive: Pulsing[A]): Unit = {
    Observe(reactive) { value =>
      log(reactive + " now " + value)
    }
  }

}

object PhilosopherTable {
  val names = Random.shuffle(
    List("Agripina", "Alberto", "Alverta", "Beverlee", "Bill", "Bobby", "Brandy", "Caleb", "Cami", "Candice", "Candra",
      "Carter", "Cassidy", "Corene", "Danae", "Darby", "Debi", "Derrick", "Douglas", "Dung", "Edith", "Eleonor",
      "Eleonore", "Elvera", "Ewa", "Felisa", "Fidel", "Filiberto", "Francesco", "Georgia", "Glayds", "Hal", "Jacque",
      "Jeff", "Joane", "Johnny", "Lai", "Leeanne", "Lenard", "Lita", "Marc", "Marcelina", "Margret", "Maryalice",
      "Michale", "Mike", "Noriko", "Pete", "Regenia", "Rico", "Roderick", "Roxie", "Salena", "Scottie", "Sherill",
      "Sid", "Steve", "Susie", "Tyrell", "Viola", "Wilhemina", "Zenobia"))


  // ============================================= Infrastructure ========================================================

  sealed trait Philosopher
  case object Thinking extends Philosopher
  case object Hungry extends Philosopher

  sealed trait Fork
  case object Free extends Fork
  case class Taken(name: String) extends Fork

  sealed trait Vision
  case object Ready extends Vision
  case object Eating extends Vision
  case class WaitingFor(name: String) extends Vision


  // ============================================ Entity Creation =========================================================

  case class Seating(placeNumber: Int, philosopher: Var[Philosopher], leftFork: Signal[Fork], rightFork: Signal[Fork], vision: Signal[Vision])


  @tailrec // unrolled into loop by compiler
  final def repeatUntilTrue(op: => Boolean): Unit = if (!op) repeatUntilTrue(op)


}
