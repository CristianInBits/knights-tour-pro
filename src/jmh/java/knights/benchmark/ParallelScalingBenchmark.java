package knights.benchmark;

import knights.model.Board;
import knights.model.Position;
import knights.solver.BacktrackingSolver;
import knights.solver.TourSolver;
import knights.solver.parallel.ParallelBacktrackingSolver;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime) // tiempo medio por ejecución
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Timeout(time = 20, timeUnit = TimeUnit.SECONDS) // seguridad
@State(Scope.Benchmark)
public class ParallelScalingBenchmark {

    @Param({ "7x7", "8x8" })
    public String boardSize;

    @Param({ "corner", "center" })
    public String startPos;

    @Param({ "normal", "parallel" })
    public String strategy;

    @Param({ "1", "2", "3" })
    public int forkDepth; // solo aplica a parallel

    @Param({ "1", "2", "4", "8" })
    public int parallelism; // solo aplica a parallel

    private int rows, cols;
    private Position start;

    @Setup(Level.Iteration)
    public void setup() {
        String[] parts = boardSize.split("x");
        rows = Integer.parseInt(parts[0]);
        cols = Integer.parseInt(parts[1]);

        if ("corner".equals(startPos)) {
            start = new Position(0, 0);
        } else {
            start = new Position(rows / 2, cols / 2);
        }
    }

    @Benchmark
    public List<Position> openTour_firstSolution() {
        Board board = new Board(rows, cols);
        boolean isClosed = false; // solo tours abiertos

        if ("normal".equals(strategy)) {
            TourSolver solver = new BacktrackingSolver(board, start, isClosed);
            return solver.solve();
        } else {
            ForkJoinPool pool = new ForkJoinPool(parallelism);
            try {
                ParallelBacktrackingSolver p = new ParallelBacktrackingSolver(board, start, isClosed, forkDepth, pool);
                return p.solve();
            } finally {
                pool.shutdown();
            }
        }
    }
}
