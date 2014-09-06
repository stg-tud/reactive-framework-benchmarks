# 2 Threads, no STM

    [info] Benchmark                                  Mode   Samples        Score  Score error    Units
    [info] d.t.s.r.b.SimpleREScalaShared.run          avgt         5        1,814        0,347    us/op
    [info] d.t.s.r.b.SimpleREScalaThreadLocal.run     avgt         5        2,718        0,887    us/op
    [info] d.t.s.r.b.SimpleSIDUPShared.run            avgt         5        9,234        0,866    us/op
    [info] d.t.s.r.b.SimpleSIDUPThreadLocal.run       avgt         5        3,038        0,122    us/op

# 2 Threads, STM

    [info] Benchmark                                  Mode   Samples        Score  Score error    Units
    [info] d.t.s.r.b.SimpleREScalaShared.run          avgt         5       20,761        6,823    us/op
    [info] d.t.s.r.b.SimpleREScalaThreadLocal.run     avgt         5        7,200       12,480    us/op
    [info] d.t.s.r.b.SimpleSIDUPShared.run            avgt         5        9,402        1,600    us/op
    [info] d.t.s.r.b.SimpleSIDUPThreadLocal.run       avgt         5        2,950        0,156    us/op

# 1 Thread, STM

    [info] Benchmark                                  Mode   Samples        Score  Score error    Units
    [info] d.t.s.r.b.SimpleREScalaShared.run          avgt         5        4,807        0,321    us/op
    [info] d.t.s.r.b.SimpleREScalaThreadLocal.run     avgt         5        4,922        2,446    us/op
    [info] d.t.s.r.b.SimpleSIDUPShared.run            avgt         5        2,532        0,099    us/op
    [info] d.t.s.r.b.SimpleSIDUPThreadLocal.run       avgt         5        2,607        0,134    us/op

# 1 Thread, no STM

    [info] Benchmark                                  Mode   Samples        Score  Score error    Units
    [info] d.t.s.r.b.SimpleREScalaShared.run          avgt         5        1,051        0,066    us/op
    [info] d.t.s.r.b.SimpleREScalaThreadLocal.run     avgt         5        1,349        2,635    us/op
    [info] d.t.s.r.b.SimpleSIDUPShared.run            avgt         5        2,558        0,066    us/op
    [info] d.t.s.r.b.SimpleSIDUPThreadLocal.run       avgt         5        2,599        0,123    us/op
