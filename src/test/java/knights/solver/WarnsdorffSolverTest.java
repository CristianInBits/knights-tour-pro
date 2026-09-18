package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WarnsdorffSolver.
 * Notes:
 * - Comments are in English as requested.
 * - We prefer tiny/impossible boards for fast deterministic checks,
 * plus a small positive case (5x5 open from corner) where Warnsdorff usually
 * succeeds.
 */
class WarnsdorffSolverTest {

    // ===== Helpers =====

    private void assertKnightAdjacencyChain(List<Position> tour) {
        // Every consecutive pair must be a legal knight move
        for (int i = 1; i < tour.size(); i++) {
            Position a = tour.get(i - 1);
            Position b = tour.get(i);
            assertTrue(a.isAdjacent(b),
                    "Consecutive positions must be knight-adjacent: " + a + " -> " + b);
        }
    }

    private void assertAllDistinct(List<Position> tour) {
        // No cell should repeat within the tour
        Set<Position> set = new HashSet<>(tour);
        assertEquals(tour.size(), set.size(), "Tour must not revisit any cell");
    }

    private void assertBoardConsistent(Board board, List<Position> tour) {
        // Board marks must match the tour order exactly
        int n = tour.size();
        assertEquals(n, board.getSteps(), "Board steps must match tour length");
        int[][] marks = board.getPath();
        for (int i = 0; i < n; i++) {
            Position p = tour.get(i);
            assertEquals(i, marks[p.row()][p.col()],
                    "Board mark must match tour index at " + p);
        }
    }

    // ===== Tests =====

    @Test
    @Timeout(1)
    void testOpen1x1_hasSingleTrivialTour() {
        int rows = 1, cols = 1;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        TourSolver solver = new WarnsdorffSolver(board, start, /* closed= */false);
        List<Position> tour = solver.solve();

        assertFalse(tour.isEmpty(), "Open 1x1 must have a trivial tour");
        assertEquals(1, tour.size(), "Open 1x1 tour length must be 1");
        assertEquals(start, tour.get(0), "Tour must start at the given position");

        assertAllDistinct(tour);
        // Adjacency chain is vacuously true for length 1
        assertBoardConsistent(board, tour);
    }

    @Test
    @Timeout(1)
    void testClosed1x1_hasNoTour() {
        int rows = 1, cols = 1;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        TourSolver solver = new WarnsdorffSolver(board, start, /* closed= */true);
        List<Position> tour = solver.solve();

        assertTrue(tour.isEmpty(), "Closed 1x1 must have no tour");
    }

    @Test
    @Timeout(1)
    void testOpen2x3_hasNoTour() {
        int rows = 2, cols = 3;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        TourSolver solver = new WarnsdorffSolver(board, start, /* closed= */false);
        List<Position> tour = solver.solve();

        assertTrue(tour.isEmpty(), "Open 2x3 must have no tour");
    }

    @Test
    @Timeout(1)
    void testOpen4x4_hasNoTour() {
        int rows = 4, cols = 4;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        TourSolver solver = new WarnsdorffSolver(board, start, /* closed= */false);
        List<Position> tour = solver.solve();

        assertTrue(tour.isEmpty(), "Open 4x4 must have no tour");
    }

    @Test
    @Timeout(2)
    void testOpen5x5FromCorner_warnsdorffUsuallyFindsTour() {
        // Typical positive case: Warnsdorff with deterministic tie-break should find a
        // full tour.
        int rows = 5, cols = 5;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        TourSolver solver = new WarnsdorffSolver(board, start, /* closed= */false);
        List<Position> tour = solver.solve();

        assertFalse(tour.isEmpty(), "Warnsdorff should usually find a tour on 5x5 from a corner");
        assertEquals(rows * cols, tour.size(), "Tour must visit all cells");
        assertEquals(start, tour.get(0), "Tour must start at the given position");

        assertAllDistinct(tour);
        assertKnightAdjacencyChain(tour);
        assertBoardConsistent(board, tour);
    }

    @Test
    @Timeout(2)
    void testDeterministicSelection_samePathAcrossRuns() {
        // With a deterministic tie-break in Warnsdorff, two fresh runs should produce
        // the same path.
        int rows = 5, cols = 5;
        Position start = new Position(0, 0);

        List<Position> t1 = new WarnsdorffSolver(new Board(rows, cols), start, false).solve();
        List<Position> t2 = new WarnsdorffSolver(new Board(rows, cols), start, false).solve();

        // Both either empty or both full, and if full, exactly the same sequence.
        assertEquals(t1.isEmpty(), t2.isEmpty(), "Both runs should either find or not find a tour");
        if (!t1.isEmpty()) {
            assertEquals(t1.size(), t2.size(), "Tour lengths must match");
            assertEquals(t1, t2, "Deterministic tie-break should yield identical tours");
        }
    }
}
