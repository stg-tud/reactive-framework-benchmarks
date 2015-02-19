package benchmarks.grid

import interface.ReactiveInterface

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable


case class Pos(x: Int, y: Int) {
  def inBounds(target: Pos) = 0 <= x && 0 <= y && x <= target.x && y <= target.y
}

class Grid(val RI: ReactiveInterface, size: Pos, connections: Pos => List[Pos]) {

  type Var = Grid.this.RI.IVar[String]
  type Signal = Grid.this.RI.ISignal[String]
  type Row = mutable.ArrayBuffer[Signal]

  val sources: IndexedSeq[Var] = Range(0, size.x).map(i => RI.makeVar(f"$i"))

  val rows: mutable.ArrayBuffer[Row] = {
    val r = mutable.ArrayBuffer[Row]()
    r += mutable.ArrayBuffer(sources: _*)
    Range(1, size.y).foreach { y =>
      val row: Row = mutable.ArrayBuffer()
      r += row
      Range(0, size.x).foreach { x =>
        val positions = connections(Pos(x, y)).filter(_.inBounds(Pos(x, y)))
        val dependencies = positions.map(p => r(p.y)(p.x))
        row += RI.combineSeq(dependencies)(_.mkString(", "))
      }
    }
    r
  }
}
