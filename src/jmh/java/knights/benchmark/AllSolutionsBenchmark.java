package knights.benchmark;

import knights.model.Board;
import knights.model.Position;
import knights.solver.AllToursSolver;
import knights.solver.BacktrackingAllSolutionsSolver;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Measures full enumeration cost using BacktrackingAllSolutionsSolver.
 * WARNING: keep sizes small; the number of tours can explode.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Thread)
public class AllSolutionsBenchmark {

    @Param({ "5" }) // keep small; use with caution
    public int size;

    @Param({ "open" }) // "closed" if you want, but watch out for runtime
    public String tourType;

    @Param({ "corner" }) // start position for determinism
    public String startPos;

    private AllToursSolver solver;
    private int expectedCells;

    @Setup(Level.Iteration)
    public void setup() {
        boolean closed = "closed".equalsIgnoreCase(tourType);
        Position start = "center".equalsIgnoreCase(startPos)
                ? new Position(size / 2, size / 2)
                : new Position(0, 0);
        solver = new BacktrackingAllSolutionsSolver(new Board(size, size), start, closed);
        expectedCells = size * size;
    }

    @Benchmark
    public int enumerateAll(Blackhole bh) {
        List<List<Position>> all = solver.solveAll();
        // Light validation and consumption
        for (List<Position> tour : all) {
            if (tour.size() != expectedCells) {
                throw new IllegalStateException("Invalid tour length: " + tour.size());
            }
            bh.consume(tour);
        }
        return all.size(); // return count to be visible to JMH
    }
}
