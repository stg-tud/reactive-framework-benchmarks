package interface

import reactive.signals.Signal

import scala.collection.immutable.Seq
import scala.language.higherKinds

/**
 * this tries to create some common abstractions for reactive implementations
 * to make implementing benchmarks for different frameworks easier
 */
trait ReactiveInterface[ISignal[_], ISignalSource[_], IEvent[_], IEventSource[_]] {
  def mapSignal[I, O](signal: ISignal[I])(f: I => O): ISignal[O]

  def setSignal[V](source: ISignalSource[V])(value: V): Unit

  def setSignals[V](changes: (ISignalSource[V], V)*): Unit

  def getSignal[V](sink: ISignal[V]): V

  def makeSignal[V](value: V): ISignalSource[V]

  def combineSeq[V, R](signals: Seq[ISignal[V]])(f: Seq[V] => R): ISignal[R]
  def combine2[A1, A2, R](s1: ISignal[A1], s2: ISignal[A2])(f: (A1, A2) => R): ISignal[R]
  def combine3[A1, A2, A3, R](s1: ISignal[A1], s2: ISignal[A2], s3: ISignal[A3])(f: (A1, A2, A3) => R): ISignal[R]
}

object ReactiveInterface {
  val sidup: ReactiveInterface[reactive.signals.Signal, reactive.signals.Var, reactive.events.EventStream, reactive.events.EventSource] = {
    import reactive.signals.{Signal, Var}
    import reactive.events.{EventSource, EventStream}
    import scala.concurrent.stm.atomic
    new ReactiveInterface[Signal, Var, EventStream, EventSource] {

      def mapSignal[I, O](signal: Signal[I])(f: (I) => O): Signal[O] = signal.single.map(f)

      def setSignal[V](source: Var[V])(value: V): Unit = setSignals(source -> value)

      def setSignals[V](changes: (Var[V], V)*): Unit = {
        val tb = new reactive.TransactionBuilder
        changes.foreach { case (source, v) => tb.set(source, v) }
        tb.commit()
      }

      def getSignal[V](sink: Signal[V]): V = sink.single.now

      def makeSignal[V](value: V): Var[V] = Var(value)

      def transpose[V](signals: Seq[Signal[V]]): Signal[Seq[V]] = atomic { tx =>
        new reactive.signals.impl.FunctionalSignal({
          inTx => signals.map { _.now(inTx) }
        }, signals, tx)
      }

      override def combineSeq[V, R](signals: Seq[Signal[V]])(f: Seq[V] => R): Signal[R] = atomic { tx =>
        new reactive.signals.impl.FunctionalSignal({
          inTx => f(signals.map(_.now(inTx)))
        }, signals, tx)
      }

      override def combine2[A1, A2, R](s1: Signal[A1], s2: Signal[A2])(f: (A1, A2) => R): Signal[R] = reactive.Lift.single.signal2(f)(s1, s2)
      override def combine3[A1, A2, A3, R](s1: Signal[A1], s2: Signal[A2], s3: Signal[A3])(f: (A1, A2, A3) => R): Signal[R] = reactive.Lift.single.signal3(f)(s1, s2, s3)
    }
  }

  val scalaRx: ReactiveInterface[rx.Rx, rx.Var, rx.Rx, rx.Var] = {
    import rx._

    new ReactiveInterface[Rx, Var, Rx, Var] {
      def mapSignal[I, O](signal: Rx[I])(f: (I) => O): Rx[O] = Rx(f(signal()))

      def setSignal[V](source: rx.Var[V])(value: V): Unit = source() = value

      def setSignals[V](changes: (rx.Var[V], V)*): Unit = changes.foreach { case (source, v) => source() = v }

      def getSignal[V](sink: Rx[V]): V = sink()

      def makeSignal[V](value: V): rx.Var[V] = rx.Var(value)

      def transpose[V](signals: Seq[Rx[V]]): Rx[Seq[V]] = Rx { signals.map(_()) }

      def combineSeq[V, R](signals: Seq[Rx[V]])(f: (Seq[V]) => R): Rx[R] = Rx { f(signals.map(_())) }

      override def combine2[A1, A2, R](s1: Rx[A1], s2: Rx[A2])(f: (A1, A2) => R): Rx[R] = Rx(f(s1(), s2()))
      override def combine3[A1, A2, A3, R](s1: Rx[A1], s2: Rx[A2], s3: Rx[A3])(f: (A1, A2, A3) => R): Rx[R] = Rx(f(s1(), s2(), s3()))

    }
  }

  class WrappedDomain extends scala.react.Domain {
    val scheduler = new ManualScheduler()
    val engine = new Engine()
  }

  def scalaReact(domain: scala.react.Domain = new WrappedDomain()): ReactiveInterface[domain.type#Signal, domain.type#Var, domain.type#Events, domain.type#EventSource] = {
    new ReactiveInterface[domain.type#Signal, domain.type#Var, domain.type#Events, domain.type#EventSource] {
      def mapSignal[I, O](signal: domain.type#Signal[I])(f: (I) => O): domain.type#Signal[O] = {
        var result: Option[domain.type#Signal[O]] = None
        domain.schedule {
          result = Some { domain.Strict { f(signal()) } }
        }
        domain.runTurn(())
        result.get
      }

      val observer = new domain.Observing {}

      def getSignal[V](sink: domain.type#Signal[V]): V = sink.getValue

      def setSignal[V](source: domain.type#Var[V])(value: V): Unit = setSignals(source -> value)

      def setSignals[V](changes: (domain.type#Var[V], V)*): Unit = {
        domain.schedule {
          changes.foreach { case (source, v) => source() = v }
        }
        domain.runTurn(())
      }

      def makeSignal[V](value: V): domain.type#Var[V] = domain.Var(value)(domain.owner)

      def transpose[V](signals: Seq[domain.type#Signal[V]]): domain.type#Signal[Seq[V]] = {
        var res: Option[domain.type#Signal[Seq[V]]] = None
        domain.schedule {
          res = Some { domain.Strict { signals.map(_()) } }
        }
        domain.runTurn(())
        res.get
      }

      def combineSeq[V, R](signals: Seq[domain.type#Signal[V]])(f: (Seq[V]) => R): domain.type#Signal[R] = {
        var res: Option[domain.type#Signal[R]] = None
        domain.schedule {
          res = Some { domain.Strict { f(signals.map(_())) } }
        }
        domain.runTurn(())
        res.get
      }

      override def combine2[A1, A2, R](s1: domain.type#Signal[A1], s2: domain.type#Signal[A2])(f: (A1, A2) => R): domain.type#Signal[R] = domain.Strict(f(s1(), s2()))
      override def combine3[A1, A2, A3, R](s1: domain.type#Signal[A1], s2: domain.type#Signal[A2], s3: domain.type#Signal[A3])(f: (A1, A2, A3) => R): domain.type#Signal[R] = domain.Strict(f(s1(), s2(), s3()))
    }
  }

}
