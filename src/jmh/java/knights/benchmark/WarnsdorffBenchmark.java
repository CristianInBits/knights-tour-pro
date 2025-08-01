package knights.benchmark;

import knights.model.Board;
import knights.model.Position;
import knights.solver.TourSolver;
import knights.solver.WarnsdorffSolver;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class WarnsdorffBenchmark {

    @Param({ "5", "6" })
    public int size;

    private TourSolver solver;

    @Setup(Level.Invocation)
    public void setup() {
        Board board = new Board(size, size);
        Position start = new Position(0, 0);
        solver = new WarnsdorffSolver(board, start, false);
    }

    @Benchmark
    public List<Position> solveTour() {
        return solver.solve();
    }
}
