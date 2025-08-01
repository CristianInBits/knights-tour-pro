package knights.benchmark;

import knights.model.Board;
import knights.model.Position;
import knights.solver.BacktrackingSolver;
import knights.solver.TourSolver;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class BacktrackingBenchmark {

    @Param({ "5", "6" })
    public int size;

    private TourSolver solver;

    @Setup(Level.Invocation)
    public void setup() {
        Board board = new Board(size, size);
        Position start = new Position(0, 0);
        solver = new BacktrackingSolver(board, start, false);
    }

    @Benchmark
    public List<Position> solveTour() {
        return solver.solve();
    }
}
