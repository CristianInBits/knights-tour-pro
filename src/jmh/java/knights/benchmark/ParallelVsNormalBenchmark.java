package knights.benchmark;

import knights.model.Board;
import knights.model.Position;
import knights.solver.BacktrackingSolver;
import knights.solver.TourSolver;
import knights.solver.parallel.ParallelBacktrackingSolver;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Timeout(time = 30, timeUnit = TimeUnit.SECONDS) // seguridad: mata iteraciones largas
@State(Scope.Thread)
public class ParallelVsNormalBenchmark {

    @Param({ "5x5", "6x6" })
    public String boardSize;

    @Param({ "open", "closed" })
    public String tourType;

    @Param({ "normal", "parallel" })
    public String strategy;

    @Param({ "1", "2" }) // profundidad de fork (ajústalo si quieres)
    public int forkDepth;

    private int rows, cols;
    private boolean isClosed;
    private Position start;

    @Setup(Level.Iteration)
    public void setup() {
        String[] parts = boardSize.split("x");
        rows = Integer.parseInt(parts[0]);
        cols = Integer.parseInt(parts[1]);

        isClosed = tourType.equalsIgnoreCase("closed");
        start = new Position(0, 0);
    }

    @Benchmark
    public List<Position> solveSingleTour() {
        // Corto-circuito: tour CERRADO imposible si filas*cols es impar (ej.: 5x5)
        if (isClosed && ((rows * cols) % 2 != 0)) {
            return List.of(); // evita explorar un caso imposible durante minutos
        }

        Board board = new Board(rows, cols);
        TourSolver solver;

        if ("parallel".equalsIgnoreCase(strategy)) {
            solver = new ParallelBacktrackingSolver(board, start, isClosed, forkDepth);
        } else {
            solver = new BacktrackingSolver(board, start, isClosed);
        }
        return solver.solve();
    }
}
