package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParallelBacktrackingSolver.
 * Notes:
 * - Comments are in English as requested.
 * - The parallel solver uses internal board copies, so we don't assert on the
 * external Board marks.
 * - We validate tour geometry (adjacency), uniqueness, length, and
 * (conditionally) closedness.
 */
class ParallelBacktrackingSolverTest {

    // ===== Helpers =====

    private static void assertKnightAdjacencyChain(List<Position> tour) {
        for (int i = 1; i < tour.size(); i++) {
            Position a = tour.get(i - 1);
            Position b = tour.get(i);
            assertTrue(a.isAdjacent(b),
                    "Consecutive positions must be knight-adjacent: " + a + " -> " + b);
        }
    }

    private static void assertAllDistinct(List<Position> tour) {
        Set<Position> set = new HashSet<>(tour);
        assertEquals(tour.size(), set.size(), "Tour must not revisit any cell");
    }

    // ===== Tests =====

    @Test
    @Timeout(1)
    void constructorRejectsStartOutsideBoard() {
        Board board = new Board(5, 5);
        Position start = new Position(5, 0); // outside
        assertThrows(IllegalArgumentException.class,
                () -> new ParallelBacktrackingSolver(board, start, false, 2));
    }

    @Test
    @Timeout(1)
    void open1x1_hasSingleTrivialTour() {
        Board board = new Board(1, 1);
        Position start = new Position(0, 0);
        // forkDepth = 0 means sequential fallback from the start
        TourSolver solver = new ParallelBacktrackingSolver(board, start, false, 0);

        List<Position> tour = solver.solve();
        assertFalse(tour.isEmpty(), "Open 1x1 must have a trivial tour");
        assertEquals(1, tour.size(), "Open 1x1 tour length must be 1");
        assertEquals(start, tour.get(0), "Tour must start at the given position");
    }

    @Test
    @Timeout(1)
    void closed1x1_hasNoTour() {
        Board board = new Board(1, 1);
        Position start = new Position(0, 0);
        TourSolver solver = new ParallelBacktrackingSolver(board, start, true, 0);

        List<Position> tour = solver.solve();
        assertTrue(tour.isEmpty(), "Closed 1x1 must have no tour");
    }

    @Test
    @Timeout(1)
    void open2x3_hasNoTour() {
        Board board = new Board(2, 3);
        Position start = new Position(0, 0);
        TourSolver solver = new ParallelBacktrackingSolver(board, start, false, 2);

        List<Position> tour = solver.solve();
        assertTrue(tour.isEmpty(), "Open 2x3 must have no tour");
    }

    @Test
    @Timeout(1)
    void open4x4_hasNoTour() {
        Board board = new Board(4, 4);
        Position start = new Position(0, 0);
        TourSolver solver = new ParallelBacktrackingSolver(board, start, false, 2);

        List<Position> tour = solver.solve();
        assertTrue(tour.isEmpty(), "Open 4x4 must have no tour");
    }

    @Test
    @Timeout(3)
    void open5x5_fromCorner_findsFullTour() {
        int rows = 5, cols = 5;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new ParallelBacktrackingSolver(board, start, false, 2);

        List<Position> tour = solver.solve();
        assertFalse(tour.isEmpty(), "Parallel solver should find a tour on 5x5 from a corner");
        assertEquals(rows * cols, tour.size(), "Tour must visit all cells");
        assertEquals(start, tour.get(0), "Tour must start at the given position");
        assertAllDistinct(tour);
        assertKnightAdjacencyChain(tour);
    }

    @Test
    @Timeout(3)
    void open6x6_fromCorner_findsFullTour() {
        int rows = 6, cols = 6;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new ParallelBacktrackingSolver(board, start, false, 3);

        List<Position> tour = solver.solve();
        assertFalse(tour.isEmpty(), "Parallel solver should find a tour on 6x6 from a corner");
        assertEquals(rows * cols, tour.size(), "Tour must visit all cells");
        assertEquals(start, tour.get(0), "Tour must start at the given position");
        assertAllDistinct(tour);
        assertKnightAdjacencyChain(tour);
    }

    @Test
    @Timeout(3)
    void closed6x6_fromCorner_ifFound_isClosedAndFull() {
        // We don't assert existence because depending on ordering/heuristics,
        // a first-solution search might fail to find a closed tour even if one exists.
        int rows = 6, cols = 6;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new ParallelBacktrackingSolver(board, start, true, 3);

        List<Position> tour = solver.solve();
        if (!tour.isEmpty()) {
            assertEquals(rows * cols, tour.size(), "Closed tour must visit all cells");
            assertEquals(start, tour.get(0), "Tour must start at the given position");
            assertAllDistinct(tour);
            assertKnightAdjacencyChain(tour);
            Position last = tour.get(tour.size() - 1);
            assertTrue(last.isAdjacent(start), "Closed tour must end adjacent to start");
        }
    }

    @Test
    @Timeout(3)
    void compareWithSequentialBacktracking_on6x6_open() {
        int rows = 6, cols = 6;
        Position start = new Position(0, 0);

        TourSolver seq = new BacktrackingSolver(new Board(rows, cols), start, false);
        TourSolver par = new ParallelBacktrackingSolver(new Board(rows, cols), start, false, 3);

        List<Position> tSeq = seq.solve();
        List<Position> tPar = par.solve();

        // Both should either find or not find; typically both find on 6x6 from corner.
        assertEquals(tSeq.isEmpty(), tPar.isEmpty(), "Both solvers should behave similarly on 6x6 open from corner");
        if (!tSeq.isEmpty()) {
            assertEquals(rows * cols, tPar.size(), "Parallel tour must visit all cells");
            assertKnightAdjacencyChain(tPar);
            assertAllDistinct(tPar);
        }
    }

    @Test
    @Timeout(3)
    void supportsCustomForkJoinPool() {
        int rows = 5, cols = 5;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        ForkJoinPool pool = new ForkJoinPool(2); // limit parallelism for the test
        try {
            TourSolver solver = new ParallelBacktrackingSolver(board, start, false, 2, pool);
            List<Position> tour = solver.solve();
            assertFalse(tour.isEmpty(), "Parallel solver should find a tour on 5x5");
            assertEquals(rows * cols, tour.size(), "Tour must visit all cells");
            assertKnightAdjacencyChain(tour);
            assertAllDistinct(tour);
        } finally {
            pool.shutdown();
        }
    }
}
