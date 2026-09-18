package knights.benchmark;

import knights.model.Board;
import knights.model.Position;
import knights.solver.AllToursSolver;
import knights.solver.BacktrackingAllSolutionsSolver;
import knights.solver.ParallelAllToursSolver;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Full enumeration, sequential against parallel.
 *
 * This is the one case where parallelism does what people expect it to. Finding a single
 * tour can stop the moment one turns up, so the threads mostly buy luck; here every branch
 * has to be walked whatever happens, so they are dividing a fixed amount of work and the
 * gain is bounded by the core count rather than by which branch happened to pay off.
 *
 * The board is 5x6: 4542 tours, large enough to give the threads something to do and small
 * enough to hold in memory. Keep it small — the number of tours grows explosively, and
 * every one of them is kept.
 *
 * AverageTime reports ms/op, so lower is better.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class AllSolutionsBenchmark {

    private static final int EXPECTED_TOURS = 4542;

    @Param({ "5" })
    public int rows;

    @Param({ "6" })
    public int cols;

    @Param({ "open" })
    public String tourType;

    @Param({ "backtrack", "parallel" })
    public String strategy;

    /** Ignored by the sequential enumerator; the depth the parallel one splits to. */
    @Param({ "3" })
    public int forkDepth;

    private Position start;
    private boolean closed;

    @Setup(Level.Iteration)
    public void setup() {
        start = new Position(0, 0);
        closed = "closed".equalsIgnoreCase(tourType);
    }

    @Benchmark
    public int enumerateAll(Blackhole bh) {
        AllToursSolver solver = "parallel".equalsIgnoreCase(strategy)
                ? new ParallelAllToursSolver(new Board(rows, cols), start, closed, forkDepth)
                : new BacktrackingAllSolutionsSolver(new Board(rows, cols), start, closed);

        List<List<Position>> all = solver.solveAll();
        bh.consume(all);

        // Light validation: a split that loses or repeats a branch shows up as a count
        // that no longer matches, which is the regression most worth catching here.
        if (rows == 5 && cols == 6 && !closed && all.size() != EXPECTED_TOURS) {
            throw new IllegalStateException("Expected " + EXPECTED_TOURS + " tours, got " + all.size());
        }
        return all.size();
    }
}
