package philosophers

import java.util.concurrent.TimeoutException

import rescala.Signals.lift
import rescala.{Var, Signal, Observe, DependentUpdate}
import rescala.graph.Pulsing
import rescala.turns.Engines.pessimistic

import scala.collection.immutable.IndexedSeq
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}



object REScalaPhilosophers extends App {
  val size = 3

  // ============================================= Infrastructure ========================================================

  sealed trait Philosopher
  case object Thinking extends Philosopher
  case object Eating extends Philosopher

  sealed trait Fork
  case object Free extends Fork
  case object Occupied extends Fork
  case object DoubleUsageError extends Fork

  val calcFork = { (leftState: Philosopher, rightState: Philosopher) =>
    if (leftState == Eating && rightState == Eating) {
      DoubleUsageError
    } else if (leftState == Eating || rightState == Eating) {
      Occupied
    } else {
      Free
    }
  }
  val calcReady = { (leftState: Fork, rightState: Fork) =>
    leftState == Free && rightState == Free
  }

  // ============================================ Entity Creation =========================================================

  case class Seating(placeNumber: Integer, philosopher: Var[Philosopher], leftFork: Signal[Fork], rightFork: Signal[Fork], canEat: Signal[Boolean])
  def createTable(tableSize: Int): Seq[Seating] = {
    val phils: IndexedSeq[Var[Philosopher]] = for (i <- 0 until tableSize) yield {
      Var[Philosopher](Thinking)
    }
    val forks = for (i <- 0 until tableSize) yield {
      lift(phils(i), phils((i + 1) % tableSize))(calcFork)
    }
    val canEat = for (i <- 0 until tableSize) yield {
      lift(forks(i), forks((i - 1 + tableSize) % tableSize))(calcReady)
    }
    for (i <- 0 until tableSize) yield {
      Seating(i, phils(i), forks(i), forks((i - 1 + tableSize) % tableSize), canEat(i))
    }
  }

  val seatings = createTable(size)
  val phils = seatings.map { _.philosopher }

  // ============================================== Logging =======================================================

  def log(msg: String): Unit = {
    println("[" + Thread.currentThread().getName + " @ " + System.currentTimeMillis() + "] " + msg)
  }
  def log[A](reactive: Pulsing[A]): Unit = {
    Observe(reactive) { value =>
      log(reactive + " now " + value)
    }
  }

  seatings.foreach { seating =>
    log(seating.philosopher)
    log(seating.leftFork)
    // right fork is the next guy's left fork
  }

  // ============================================ Runtime Behavior  =========================================================

  phils.foreach { philosopher =>
    philosopher.observe { state =>
      if (state == Eating)
        Future {
          philosopher set Thinking
        }
    }
  }

  @annotation.tailrec // unrolled into loop by compiler
  def repeatUntilTrue(op: => Boolean): Unit = if (!op) repeatUntilTrue(op)

  def eatOnce(seating: Seating) = {
    repeatUntilTrue {
      DependentUpdate(seating.canEat) {
        (writes, canEat) =>
          if (canEat) {
            writes += seating.philosopher -> Eating
            true // Don't try again
          } else {
            false // Try again
          }
      }
    }
  }

  // ============================================== Thread management =======================================================

  // ===================== STARTUP =====================
  // start simulation
  @volatile private var killed = false
  log("Starting simulation. Press <Enter> to terminate!")
  val threads = seatings.map { seating =>
    val phil = seating.philosopher
    phil ->
      Future {
        log("Controlling hunger on " + seating)
        while (!killed) {
          eatOnce(seating)
        }
        log(phil + " dies.")
      }
  }

  // ===================== SHUTDOWN =====================
  // wait for keyboard input
  System.in.read()

  // kill all philosophers
  log("Received Termination Signal, Terminating...")
  killed = true

  // collect forked threads to check termination
  threads.foreach {
    case (phil, thread) => try {
      import scala.language.postfixOps
      Await.ready(thread, 50 millis)
      log(phil + " terminated.")
    } catch {
      case te: TimeoutException => log(phil + " failed to terminate!")
    }
  }
}