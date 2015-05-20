package benchmarks.STMBank

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger

import benchmarks.{EngineParam, Workload}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{Blackhole, BenchmarkParams, ThreadParams}
import rescala._
import rescala.turns.{Engine, Ticket, Turn}

import scala.concurrent.stm.{atomic, Ref}


@State(Scope.Benchmark)
class ReactiveState {


  @Param(Array("64"))
  var numberOfAccounts: Int = _

  @Param(Array("0.01"))
  var globalReadChance: Double = _

  var accounts: Array[Var[Int]] = _
  var engine: Engine[Turn] = _

  @Setup(Level.Iteration)
  def setup(params: BenchmarkParams, engine: EngineParam) = {
    this.engine = engine.engine
    val threads = params.getThreads
    implicit val e = this.engine

    accounts = Range(0, numberOfAccounts).map(_ => Var(0)).toArray
  }
}


@State(Scope.Benchmark)
class STMState {


  @Param(Array("64"))
  var numberOfAccounts: Int = _

  @Param(Array("0.01"))
  var globalReadChance: Double = _

  var accounts: Array[Ref[Int]] = _

  @Setup(Level.Iteration)
  def setup(params: BenchmarkParams) = {
    val threads = params.getThreads
    accounts = Range(0, numberOfAccounts).map(_ => Ref(0)).toArray
  }
}




@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(2)
class BankAccounts {

  @Benchmark
  def reactive(rs: ReactiveState, bh: Blackhole) = {
    val tlr = ThreadLocalRandom.current()
    if (tlr.nextDouble() < rs.globalReadChance) {
      rs.engine.plan(rs.accounts: _*){ t =>
        val sum = rs.accounts.foldLeft(0)((acc, v) => acc + v.get(t))
        bh.consume(sum)
        assert(sum == 0)
      }
    }
    else {
      val a1 = tlr.nextInt(rs.numberOfAccounts)
      val a2 = tlr.nextInt(rs.numberOfAccounts)
      val account1 = rs.accounts(a1)
      val account2 = rs.accounts(a2)
      rs.engine.plan(account1, account2){ t =>
        account1.admit(account1.get(t) + 4817)(t)
        account2.admit(account2.get(t) - 4817)(t)
      }
    }
  }


  @Benchmark
  def stm(rs: STMState, bh: Blackhole) = {
    val tlr = ThreadLocalRandom.current()
    if (tlr.nextDouble() < rs.globalReadChance) {
      atomic { t =>
        val sum = rs.accounts.foldLeft(0)((acc, v) => acc + v.get(t))
        bh.consume(sum)
        assert(sum == 0)
      }
    }
    else {
      val a1 = tlr.nextInt(rs.numberOfAccounts)
      val a2 = tlr.nextInt(rs.numberOfAccounts)
      val account1 = rs.accounts(a1)
      val account2 = rs.accounts(a2)
      atomic { t =>
        account1.set(account1.get(t) + 4817)(t)
        account2.set(account2.get(t) - 4817)(t)
      }
    }
  }



}
