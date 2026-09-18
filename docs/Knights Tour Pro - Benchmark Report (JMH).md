# Knight's Tour Pro — Benchmark Report (JMH)

## What this measures

`ParallelBacktrackingSolver` differs from `BacktrackingSolver` in two independent ways:
it orders candidate moves by Warnsdorff degree, and it explores several branches at once.
Comparing the two solvers head to head measures both changes together, which makes it easy
to credit the wrong one.

So the benchmark runs three variants:

| Variant | What it is |
| ------- | ---------- |
| `naive_order_sequential` | `BacktrackingSolver` — moves tried in board order, one thread |
| `warnsdorff_order_sequential` | `ParallelBacktrackingSolver` with `forkDepth = 0` — same ordering as the parallel solver, still one thread |
| `warnsdorff_order_parallel` | `ParallelBacktrackingSolver` with `forkDepth = 4` |

The gap between the first two is what the **heuristic** buys. The gap between the last two
is what the **threads** buy.

Two scenarios, because the answer reverses between them:

* **open-from-centre** — 6×6, open tour, starting at (3,3).
* **closed-from-corner** — 6×6, closed tour, starting at (0,0).

## Setup

* JMH 1.37, `Mode.AverageTime`, ms/op — **lower is better**.
* 1 fork, 2 warmup iterations, 3 measurement iterations, 2 s each.
* JDK 17.0.20.1 (Temurin, resolved by the Gradle toolchain), 12 available processors.
* A fresh `Board` and solver per invocation, so the numbers include allocation, not just search.

Reproduce with:

```bash
./gradlew jmh -PjmhInclude='.*ParallelVsSequentialBenchmark.*'
```

## Results

| Variant | open-from-centre | closed-from-corner |
| ------- | ---------------: | -----------------: |
| `naive_order_sequential` | 10.751 ± 0.189 ms | 29.115 ± 1.224 ms |
| `warnsdorff_order_sequential` | **0.013 ± 0.001 ms** | 6285 ± 727 ms |
| `warnsdorff_order_parallel` | 0.034 ± 0.001 ms | **0.044 ± 0.001 ms** |

## Reading the numbers

### The heuristic is worth ~800× on open tours, and is a disaster on closed ones

On the open tour the Warnsdorff ordering takes 10.751 ms down to 0.013 ms — roughly
**830× faster**, on a single thread, with no parallelism involved at all.

On the closed tour the same ordering goes from 29 ms to about 6.3 seconds: **~216× slower**
than trying moves in plain board order. Warnsdorff greedily heads for the squares with
fewest onward moves, which is excellent for covering the board but says nothing about
ending up adjacent to the start. It walks the search into a large region of paths that
cover all 36 cells and fail to close, and a single thread has to back out of that region
one dead end at a time.

### The threads are not a speed-up, they are an escape hatch

On the open tour, forking makes things **2.6× slower** (0.013 → 0.034 ms). There is almost
no search left to divide, so all that is left is the cost of deep-copying a `Board` per task
and driving the fork/join pool.

On the closed tour, forking turns ~6.3 seconds into 0.044 ms. That is a factor of roughly
**143 000× on 12 cores** — which is the point worth pausing on, because dividing work among
12 threads cannot possibly explain more than a 12× gain.

The speed-up does not come from splitting work. It comes from **not having to trust the
heuristic**. With `forkDepth = 4` the solver explores many different opening branches at the
same time; the branch Warnsdorff ranks first is the trap, but some other branch closes almost
immediately, and the shared `found` flag stops everyone as soon as it does. Parallelism here
buys diversification, not throughput.

That also explains why the parallel solver beats plain backtracking on the closed tour
(29 ms → 0.044 ms, about **660×**) even though its move ordering is the worse of the two:
running many orderings concurrently beats committing to any single one.

## A caveat on the slow cell

`warnsdorff_order_sequential` on the closed tour reads **6285 ± 727 ms**, an error of about
12%. With an operation that takes several seconds, three measurement iterations cannot do
much better. Tighten it at the cost of a longer run:

```bash
./gradlew jmh -PjmhInclude='.*ParallelVsSequentialBenchmark.*' -PjmhIterations=10
```

Every other cell has an error under 8% of its value.

## Enumerating every tour

Finding one tour and finding all of them are different problems, and they behave
differently under threads. A single-tour search can stop the moment an answer appears, so
what the threads really buy is a chance of stumbling onto a good branch. Enumeration has no
early exit: every branch is walked whatever happens, so the threads divide a fixed amount of
work and the gain is bounded by the core count.

`AllSolutionsBenchmark`, 5×6 open from the corner, which has 4542 tours:

| Strategy | Time |
| -------- | ---: |
| `backtrack` (sequential) | 2513 ± 344 ms |
| `parallel`, forkDepth 3 | 707 ± 287 ms |

That is about **3.5× on 12 cores**. Both error bars are wide — the parallel figure's is 40%
of its value — so treat this as "a few times faster", not as a precise ratio. Repeated
measurement outside JMH, taking the best of several runs, put it nearer 4.4×.

Why not 12×? The branches are nowhere near equal — some opening moves lead to far more
tours than others — and every forked task copies the board's marks. `forkDepth` 2 to 3 is
where it settles; 1 barely splits the work at all, and going deeper adds tasks without
adding parallelism.

The parallel enumeration returns the same tours in the same order as the sequential one,
which is what makes the two interchangeable.

## Practical guidance

* **Open tours** — use `warnsdorff`. It is the fastest option by a wide margin and it does not
  need a thread pool. Do not reach for `parallel` here; it only adds overhead.
* **Closed tours** — use `parallel`. Plain `warnsdorff` frequently fails outright (it returns no
  tour when its greedy path dead-ends), and the sequential search behind it is far too slow.
* **Enumerating every tour** — `backtrack` or `parallel`; the parallel one is a few times
  faster and returns the tours in the same order. `warnsdorff` cannot enumerate at all.
* `forkDepth` matters far more than pool size. Depth 0 disables forking entirely; 2 to 4 is
  where the closed-tour case becomes tractable.

## What the precomputed neighbour table changed

`Board` used to build the legal moves of a square on every call, allocating eight `Position`
objects and two streams each time. That happens once per search node and again for every
candidate while computing Warnsdorff degrees, so it dominated the profile. The moves are now
computed once per board size and shared by every copy of the board.

The measurements above are after that change. Before it:

| Variant | open-from-centre | closed-from-corner |
| ------- | ---------------: | -----------------: |
| `naive_order_sequential` | 75.280 ms | 192.383 ms |
| `warnsdorff_order_sequential` | 0.038 ms | ~42 000 ms |
| `warnsdorff_order_parallel` | 0.092 ms | 0.182 ms |

Every variant got between **2.7× and 7× faster**, and the ratios that this report is about
barely moved — the table removes a constant overhead, it does not change which strategy suits
which problem.

## Note on configuring JMH

Values set in the `jmh { }` block of `build.gradle.kts` are applied to every benchmark and
override its annotations. That block previously pinned `Throughput` and fixed iteration counts,
so each benchmark's own `@BenchmarkMode`, `@Warmup` and `@Measurement` were silently ignored.
It now sets no defaults, and each benchmark configures itself. The `-PjmhFork`, `-PjmhIterations`
and `-PjmhWarmup` properties still override everything for a one-off run.

---

*Measured on 2026-09-18. Earlier versions of this report described `BacktrackingBenchmark` and
`WarnsdorffBenchmark`, which do not exist in this repository, and attributed the parallel
solver's advantage to parallelism alone.*
