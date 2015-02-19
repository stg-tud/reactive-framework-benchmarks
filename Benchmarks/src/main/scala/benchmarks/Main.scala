package benchmarks

import benchmarks.grid.{Grid, Pos}
import interface.ReactiveInterface.Dot

object Main {

  def main(args: Array[String]): Unit = {
    val dot = new Dot
    new Grid(dot, Pos(8, 8), Grid.prim)

    println(dot.getDot())
  }
}
