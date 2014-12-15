package benchmarks


object Spawn {
  def apply(name: String)(f: => Unit): Thread = {
    val t = new Thread(new Runnable {
      override def run(): Unit = f
    }, name)
    t.start()
    t
  }
}