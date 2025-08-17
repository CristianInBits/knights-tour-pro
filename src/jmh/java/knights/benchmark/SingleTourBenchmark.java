package knights.benchmark;

import knights.model.Board;
import knights.model.Position;
import knights.solver.BacktrackingSolver;
import knights.solver.TourSolver;
import knights.solver.WarnsdorffSolver;
// Import parallel solver only if you actually have it:
// import knights.solver.ParallelBacktrackingSolver;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Measures single-solution find time for different solvers and board sizes.
 * Notes:
 * - We construct the solver once per iteration (not per invocation) to avoid
 * counting solver allocation overhead on every call. solve() resets the Board.
 * - We validate the solution size when non-empty to catch logic regressions.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
@State(Scope.Thread)
public class SingleTourBenchmark {

    @Param({ "5", "6" })
    public int size;

    @Param({ "corner", "center" })
    public String startPos; // "corner" -> (0,0), "center" -> (size/2, size/2)

    @Param({ "open" }) // you may add "closed" if you want to test closed tours too
    public String tourType;

    @Param({ "backtrack", "warnsdorff" }) // add "parallel" if available
    public String strategy;

    private Board board;
    private Position start;
    private boolean closed;
    private TourSolver solver;

    @Setup(Level.Iteration)
    public void setup() {
        board = new Board(size, size);
        start = "center".equalsIgnoreCase(startPos)
                ? new Position(size / 2, size / 2)
                : new Position(0, 0);
        closed = "closed".equalsIgnoreCase(tourType);
        solver = buildSolver(strategy, board, start, closed);
    }

    private TourSolver buildSolver(String strategy, Board b, Position s, boolean closed) {
        switch (strategy.toLowerCase()) {
            case "warnsdorff":
                return new WarnsdorffSolver(b, s, closed);
            // case "parallel":
            // return new ParallelBacktrackingSolver(b, s, closed, /*forkDepth*/ 2);
            case "backtrack":
            default:
                return new BacktrackingSolver(b, s, closed);
        }
    }

    @Benchmark
    public List<Position> solveTour(Blackhole bh) {
        // Measure the time to compute one tour from scratch
        List<Position> res = solver.solve();
        bh.consume(res);
        if (!res.isEmpty()) {
            // Validate: a real tour must visit all cells
            int expected = size * size;
            if (res.size() != expected) {
                throw new IllegalStateException("Invalid tour length: " + res.size() + " vs " + expected);
            }
        }
        return res; // returning also prevents dead-code elimination
    }

    @Benchmark
    public List<Position> solveTour_freshBoardEachTime(Blackhole bh) {
        // Alternative benchmark that also includes solver allocation/Board creation
        // overhead.
        Board b = new Board(size, size);
        Position s = "center".equalsIgnoreCase(startPos)
                ? new Position(size / 2, size / 2)
                : new Position(0, 0);
        TourSolver freshSolver = buildSolver(strategy, b, s, closed);

        List<Position> res = freshSolver.solve();
        bh.consume(res);
        if (!res.isEmpty()) {
            int expected = size * size;
            if (res.size() != expected) {
                throw new IllegalStateException("Invalid tour length: " + res.size() + " vs " + expected);
            }
        }
        return res;
    }
}
