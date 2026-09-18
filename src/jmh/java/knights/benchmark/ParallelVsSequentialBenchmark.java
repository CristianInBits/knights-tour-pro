package knights.benchmark;

import knights.model.Board;
import knights.model.Position;
import knights.solver.BacktrackingSolver;
import knights.solver.ParallelBacktrackingSolver;
import knights.solver.TourSolver;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Compare sequential vs parallel backtracking on a case that amplifies the
 * difference:
 * - Board: 6x6
 * - Start: center (size/2, size/2)
 * - Tour: open
 *
 * Why this case?
 * - Plain backtracking from the center tends to explore many branches before
 * finding a tour.
 * - Parallel backtracking can explore multiple promising branches early (via
 * forkDepth), reducing time to first solution.
 *
 * Notes:
 * - AverageTime reports ms/op (lower is better).
 * - We create fresh Board/Solver per invocation to reflect end-to-end cost
 * (allocation + search).
 * - Parallel solver uses the common ForkJoinPool; actual parallelism depends on
 * available processors.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class ParallelVsSequentialBenchmark {

    @Param({ "6" })
    public int size;

    // Keep it fixed to center for this demo; this is where the gap is most visible.
    private Position start;

    // Tune this if you want: 2–3 is a good trade-off for small boards.
    @Param({ "3" })
    public int forkDepth;

    @Setup(Level.Iteration)
    public void setup() {
        start = new Position(size / 2, size / 2); // center start
    }

    @Benchmark
    public List<Position> sequential_backtracking(Blackhole bh) {
        Board b = new Board(size, size);
        TourSolver solver = new BacktrackingSolver(b, start, /* closed= */false);
        List<Position> res = solver.solve();
        bh.consume(res);
        // Light validation to catch logic regressions without heavy overhead
        if (!res.isEmpty() && res.size() != size * size) {
            throw new IllegalStateException("Invalid tour length: " + res.size());
        }
        return res;
    }

    @Benchmark
    public List<Position> parallel_backtracking(Blackhole bh) {
        Board b = new Board(size, size);
        TourSolver solver = new ParallelBacktrackingSolver(b, start, /* closed= */false, forkDepth);
        List<Position> res = solver.solve();
        bh.consume(res);
        if (!res.isEmpty() && res.size() != size * size) {
            throw new IllegalStateException("Invalid tour length: " + res.size());
        }
        return res;
    }
}
