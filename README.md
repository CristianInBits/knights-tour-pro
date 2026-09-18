# ♞ Knight's Tour Pro

Four ways to solve the Knight's Tour — a knight visiting every square of a board exactly
once — with a command line, a JavaFX interface, and benchmarks that say which approach
actually wins and when.

---

## Quick start

You need **Java 17 or newer**. Gradle downloads everything else on the first run.

```bash
./gradlew shadowJar
java -jar build/libs/knights-tour-pro-1.0.0-all.jar 8 8 0 0 single open warnsdorff
```

That finds a tour of an 8×8 board starting from the top-left corner, prints it, and writes
it to `output/`. Or skip the packaging and use the graphical interface:

```bash
./gradlew runFx
```

---

## Choosing a strategy

| Strategy | What it does | Use it for |
| -------- | ------------ | ---------- |
| `warnsdorff` | Always steps to the square with the fewest onward moves | **Open tours.** Far and away the fastest, and it scales to large boards |
| `backtrack` | Tries every move in board order, backing up at dead ends | Enumerating **every** tour, and small boards where it is fast enough |
| `parallel` | Warnsdorff's ordering, exploring several branches at once | **Closed tours**, where a single-threaded search gets stuck for a long time |

Two things are worth knowing before you pick:

**Warnsdorff can fail.** It never backs up, so when its greedy path dead-ends it returns no
tour at all — even though one exists. That happens often on closed tours.

**`parallel` is not simply "the fast one".** On an open tour it is *slower* than the plain
sequential search, because there is nothing left to divide. On a closed tour it is faster by
orders of magnitude, and not because it splits the work: it explores several opening moves
at once, so a promising-looking branch that turns out to be a trap no longer costs you the
whole search. The [benchmark report](docs/Knights%20Tour%20Pro%20-%20Benchmark%20Report%20%28JMH%29.md)
has the measurements.

`all` mode — enumerating every tour — only works with `backtrack`. The number of tours grows
explosively, so keep the board small: a 5×5 from a corner already has 304 of them.

---

## Command line

```bash
java -jar build/libs/knights-tour-pro-1.0.0-all.jar <rows> <cols> <startRow> <startCol> <mode> <tourType> [strategy] [flags]
```

| Argument | Values | Description |
| -------- | ------ | ----------- |
| `rows`, `cols` | integers | Board size |
| `startRow`, `startCol` | integers, 0-based | Where the knight starts |
| `mode` | `single` \| `all` | One tour, or every tour |
| `tourType` | `open` \| `closed` | A closed tour ends a knight's move away from the start |
| `strategy` | `backtrack` \| `warnsdorff` \| `parallel` | Optional, defaults to `backtrack` |

| Flag | Description |
| ---- | ----------- |
| `--limit N` | In `all` mode, how many solutions to print (default 3). Does not limit the export |
| `--out DIR` | Where to write the exports (default `output`) |
| `--no-print` | Do not print boards to the console |
| `--no-export` | Do not write any files |
| `--fork-depth N` | `parallel` only: how deep to keep splitting the search (default 2) |
| `--pool N` | `parallel` only: use a dedicated pool of N threads instead of the shared one |

Flags take either form: `--out results` or `--out=results`.

### Exit codes

| Code | Meaning |
| ---- | ------- |
| `0` | A tour was found |
| `1` | No tour exists for these settings — the run was fine, the answer is simply "none" |
| `2` | The arguments are not valid |
| `3` | A file could not be written |

A board with no tour is deliberately not an error, so a script can tell "there is no
answer" from "something went wrong":

```bash
if java -jar build/libs/knights-tour-pro-1.0.0-all.jar 4 4 0 0 single open backtrack --no-print --no-export; then
  echo "found one"
elif [ $? -eq 1 ]; then
  echo "no tour on a 4x4"
fi
```

### Examples

```bash
# Every open tour of a 5x5 from the corner, printing only the first two
java -jar build/libs/knights-tour-pro-1.0.0-all.jar 5 5 0 0 all open backtrack --limit 2

# A closed tour of a 6x6, where parallel search pays off
java -jar build/libs/knights-tour-pro-1.0.0-all.jar 6 6 0 0 single closed parallel --fork-depth 4

# Straight to a file, nothing on the console
java -jar build/libs/knights-tour-pro-1.0.0-all.jar 8 8 0 0 single open warnsdorff --no-print --out results
```

Or run it through Gradle without packaging anything:

```bash
./gradlew run --args="6 6 0 0 single open backtrack"
```

---

## The graphical interface

```bash
./gradlew runFx
```

Board size, starting square, mode and strategy are all set in the window, and the knight's
path is animated square by square — the speed slider controls how fast. **Stop** interrupts
the search itself, not just the animation, so it is safe on a board that turns out to be
too big.

> The packaged JAR runs the command line only. The interface needs JavaFX's native
> libraries, so launch it with `./gradlew runFx`.

---

## Output files

Every run writes two files unless you pass `--no-export`: `tour.txt` and `tour.json` in
`single` mode, `tours.txt` and `tours.json` in `all` mode.

**`tour.txt`** — metadata sorted by name, then each step as a 1-based index and a square:

```text
Tour Export
===============
cols: 5
mode: single
rows: 5
startCol: 0
startRow: 0
strategy: warnsdorff
timestamp: 2026-09-18 21:53:17
tourType: open

Found Solutions: 1

Solution #1:
  1: (0,0)
  2: (1,2)
  ...
```

**`tour.json`** — the same information, with the tours as arrays of coordinates:

```json
{
  "metadata": {
    "rows": 5, "cols": 5, "startRow": 0, "startCol": 0,
    "tourType": "open", "mode": "single", "strategy": "warnsdorff",
    "timestamp": "2026-09-18 21:53:17"
  },
  "paths": [
    [ { "row": 0, "col": 0 }, { "row": 1, "col": 2 } ]
  ]
}
```

Runs with `parallel` add `forkDepth` and `pool` to the metadata.

---

## How it is put together

```text
src/
├── main/java/knights/
│   ├── model/      Board, Position, KnightMove
│   ├── solver/     the four strategies, behind two interfaces
│   ├── export/     TxtExporter, JsonExporter
│   ├── ui/         JavaFX interface
│   └── Main.java   command line
├── test/java/      JUnit 5
└── jmh/java/       benchmarks
```

Two interfaces keep the strategies interchangeable: `TourSolver` returns one tour (or an
empty list), and `AllToursSolver` adds `solveAll()` for enumeration. Exporters sit behind
`ResultExporter`, so adding a format means adding one class.

`Board` precomputes every square's legal moves once and shares that table with its copies,
which is what keeps the search out of the allocator — it matters because the parallel solver
copies a board for each branch it explores.

A search can be called off: the solvers check whether their thread has been interrupted and
give up, which is how the Stop button works. Cancelling raises `CancellationException`.

---

## Building and testing

```bash
./gradlew build       # compile and run the tests
./gradlew test        # tests only
./gradlew shadowJar   # package everything into one JAR
./gradlew jmh         # benchmarks (slow)
```

The build pins Java 17 through Gradle toolchains and downloads that JDK itself if you do not
have one, so it does not matter which Java you normally use. Versions live in
`build.gradle.kts`; there is no copy of them here to fall out of date.

The tests cover the four solvers, the shape of the tours they produce (length, no repeats,
legal knight moves, closing when required), the board and its neighbour table — including
the order moves come out in, which decides which tour each solver returns — cancellation,
and both exporters.

---

## Benchmarks

Three suites in `src/jmh/java`:

* **`ParallelVsSequentialBenchmark`** — separates the two things that make the parallel
  solver fast, the move ordering and the threads, since they help in opposite situations.
* **`SingleTourBenchmark`** — every strategy across board sizes and starting squares.
* **`AllSolutionsBenchmark`** — the cost of enumerating everything.

```bash
./gradlew jmh
./gradlew jmh -PjmhInclude='.*ParallelVsSequentialBenchmark.*'   # just one
```

Measurements and what they mean:
[benchmark report](docs/Knights%20Tour%20Pro%20-%20Benchmark%20Report%20%28JMH%29.md).

> Each benchmark sets its own mode and iteration counts through annotations. Anything put in
> the `jmh { }` block of `build.gradle.kts` applies to every class and silently overrides
> them, so that block is deliberately left empty.

---

## Troubleshooting

**The build fails before compiling anything.** Old Gradle versions cannot start on recent
Java releases. This project ships Gradle 9.7.1, which handles current JDKs; if you hit this
on a fork, update the wrapper:

```bash
./gradlew wrapper --gradle-version 9.7.1
```

**No output files.** Check the `--out` directory and that you have not passed `--no-export`.

**A search never finishes.** Some combinations are far harder than they look — a closed tour
on a large board from a corner, for instance. Try `parallel` with `--fork-depth 4`, or a
smaller board. In the interface, press Stop.

---

## Roadmap

Done so far: the four strategies, open and closed tours, TXT and JSON exports, the JavaFX
interface, JMH benchmarks, CI, a precomputed neighbour table, cancellable searches,
exporters that report a failed write instead of swallowing it, and meaningful exit codes
with tests covering the argument parsing.

Still open:

* Parallel enumeration of *all* tours with work stealing — today `all` mode is single-threaded.
* More export formats: CSV, and an SVG drawing of the board.
* A web front end.

---

## Contributing

Pull requests welcome. Please keep the existing style, add tests for anything new, and make
sure `./gradlew build` passes. Commit message conventions are in [CLAUDE.md](CLAUDE.md).

---

## License

MIT — see [LICENSE](LICENSE).
