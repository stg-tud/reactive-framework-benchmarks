package interface

import rescala.turns.Turn

import scala.collection.immutable.Seq
import scala.language.higherKinds


/**
 * this tries to create some common abstractions for reactive implementations
 * to make implementing benchmarks for different frameworks easier
 */
trait ReactiveInterface {

  type ISignal[_]
  type IVar[A] <: ISignal[A]
  type IEvent[_]
  type IEvt[A] <: IEvent[A]

  implicit class SignalOps[A](self: ISignal[A]) {
    def map[B](f: A => B): ISignal[B] = mapSignal(self)(f)
  }

  def mapSignal[I, O](signal: ISignal[I])(f: I => O): ISignal[O]

  def setVar[V](source: IVar[V])(value: V): Unit

  def setVars[V](changes: (IVar[V], V)*): Unit

  def getSignal[V](sink: ISignal[V]): V

  def makeVar[V](value: V): IVar[V]

  def combineSeq[V, R](signals: Seq[ISignal[V]])(f: Seq[V] => R): ISignal[R]
  def combine2[A1, A2, R](s1: ISignal[A1], s2: ISignal[A2])(f: (A1, A2) => R): ISignal[R]
  def combine3[A1, A2, A3, R](s1: ISignal[A1], s2: ISignal[A2], s3: ISignal[A3])(f: (A1, A2, A3) => R): ISignal[R]
}

object ReactiveInterface {

  def rescalaInstance(implicit engine: rescala.turns.Engine[Turn] = rescala.turns.Engines.default): ReactiveInterface = {
    import rescala._
    new ReactiveInterface {

      override type IEvt[A] = Evt[A]
      override type ISignal[A] = Signal[A]
      override type IVar[A] = Var[A]
      override type IEvent[A] = Event[A]

      def mapSignal[I, O](signal: Signal[I])(f: (I) => O): Signal[O] = signal.map(f)

      def setVar[V](source: Var[V])(value: V): Unit = source.set(value)

      def setVars[V](changes: (Var[V], V)*): Unit = engine.plan(changes.map(_._1): _*) { t => changes.foreach { case (s, v) => setVar(s)(v) } }

      def getSignal[V](sink: Signal[V]): V = sink.now

      def makeVar[V](value: V): Var[V] = Var(value)

      def transpose[V](signals: Seq[Signal[V]]): Signal[Seq[V]] = Signals.static(signals: _*) { turn =>
        signals.map(_.get(turn))
      }

      override def combineSeq[V, R](signals: Seq[Signal[V]])(f: Seq[V] => R): Signal[R] = Signals.static(signals: _*) { turn =>
        f(signals.map(_.get(turn)))
      }

      override def combine2[A1, A2, R](s1: Signal[A1], s2: Signal[A2])(f: (A1, A2) => R): Signal[R] = Signals.lift(s1, s2)(f)
      override def combine3[A1, A2, A3, R](s1: Signal[A1], s2: Signal[A2], s3: Signal[A3])(f: (A1, A2, A3) => R): Signal[R] = Signals.lift(s1, s2, s3)(f)
    }
  }

  class Dot extends ReactiveInterface {
    override type IVar[A] = Int
    override type ISignal[A] = Int
    override type IEvent[A] = Int
    override type IEvt[A] = Int

    var lines = List[String]()
    var index = 0
    def add(sink: ISignal[_])(source: ISignal[_]) = lines ::= s"$source -> $sink;"
    def comb(sources: ISignal[_]*) = {
      index += 1
      sources.foreach(add(index))
      index
    }

    def getDot(name: String = "someGraph") =
      s"""digraph $name {
         |rankdir = BT;
         |${lines.reverse.mkString("\n")}
         |}
       """.stripMargin

    override def setVar[V](source: IVar[V])(value: V): Unit = ???
    override def setVars[V](changes: (IVar[V], V)*): Unit = ???
    override def mapSignal[I, O](signal: ISignal[I])(f: (I) => O): ISignal[O] = comb(signal)
    override def getSignal[V](sink: ISignal[V]): V = ???
    override def makeVar[V](value: V): IVar[V] = {index += 1; lines ::= s"$index [shape=triangle];"; index}
    override def combineSeq[V, R](signals: Seq[ISignal[V]])(f: (Seq[V]) => R): ISignal[R] = comb(signals: _*)
    override def combine3[A1, A2, A3, R](s1: ISignal[A1], s2: ISignal[A2], s3: ISignal[A3])(f: (A1, A2, A3) => R): ISignal[R] = comb(s1,s2,s3)
    override def combine2[A1, A2, R](s1: ISignal[A1], s2: ISignal[A2])(f: (A1, A2) => R): ISignal[R] =  comb(s1,s2)
  }

}
