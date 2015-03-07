package benchmarks

import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}

import benchmarks.grid.{Grid, Pos}
import interface.ReactiveInterface.Dot

object Main {

  def main(args: Array[String]): Unit = {
    val dot = new Dot
    new Grid(dot, Pos(8, 8), Grid.prim)

    Files.write(Paths.get("prim.dot"), dot.getDot().getBytes(StandardCharsets.UTF_8))
  }
}
