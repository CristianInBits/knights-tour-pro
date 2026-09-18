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
 * Separates the two things that make ParallelBacktrackingSolver fast, because they
 * are easy to confuse and they do not help in the same situations.
 *
 * The parallel solver differs from the plain one in two independent ways: it orders
 * candidate moves by Warnsdorff degree, and it explores several branches at once.
 * Comparing it directly against BacktrackingSolver measures both at the same time
 * and credits whichever one you happen to name. So there are three variants here:
 *
 * - naive_order_sequential      plain backtracking, moves tried in board order
 * - warnsdorff_order_sequential same solver as the parallel one, forkDepth 0, so it
 *                               runs on a single thread: this isolates the ordering
 * - warnsdorff_order_parallel   the ordering plus real forking
 *
 * The gap between the first two is what the heuristic buys; the gap between the last
 * two is what the threads buy.
 *
 * Two scenarios, because the answer reverses between them:
 *
 * - open-from-centre    Warnsdorff walks almost straight to a tour, so ordering wins
 *                       and there is hardly any search left to parallelise.
 * - closed-from-corner  Warnsdorff leads the search into a region full of paths that
 *                       cover the board but fail to close, and a single thread spends
 *                       a long time backing out of it. Forking explores other opening
 *                       branches at the same time, one of which closes quickly.
 *
 * AverageTime reports ms/op, so lower is better. A fresh Board and solver are built
 * per invocation to measure the end-to-end cost a caller actually pays.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class ParallelVsSequentialBenchmark {

    private static final int SIZE = 6;

    @Param({ "open-from-centre", "closed-from-corner" })
    public String scenario;

    @Param({ "4" })
    public int forkDepth;

    private Position start;
    private boolean closed;

    @Setup(Level.Iteration)
    public void setup() {
        closed = scenario.startsWith("closed");
        start = closed ? new Position(0, 0) : new Position(SIZE / 2, SIZE / 2);
    }

    @Benchmark
    public List<Position> naive_order_sequential(Blackhole bh) {
        TourSolver solver = new BacktrackingSolver(new Board(SIZE, SIZE), start, closed);
        return consume(solver.solve(), bh);
    }

    /** Same solver as the parallel benchmark, but forkDepth 0 keeps it on one thread. */
    @Benchmark
    public List<Position> warnsdorff_order_sequential(Blackhole bh) {
        TourSolver solver = new ParallelBacktrackingSolver(new Board(SIZE, SIZE), start, closed, 0);
        return consume(solver.solve(), bh);
    }

    @Benchmark
    public List<Position> warnsdorff_order_parallel(Blackhole bh) {
        TourSolver solver = new ParallelBacktrackingSolver(new Board(SIZE, SIZE), start, closed, forkDepth);
        return consume(solver.solve(), bh);
    }

    // Light validation to catch a logic regression without adding measurable overhead.
    private List<Position> consume(List<Position> tour, Blackhole bh) {
        bh.consume(tour);
        if (tour.size() != SIZE * SIZE) {
            throw new IllegalStateException("Expected a full tour, got " + tour.size() + " cells");
        }
        return tour;
    }
}
