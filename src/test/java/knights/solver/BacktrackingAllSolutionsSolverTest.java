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
 * Tests for BacktrackingAllSolutionsSolver (enumerates all tours).
 * Notes:
 * - We only use tiny boards to keep tests fast and deterministic.
 * - We avoid asserting large known counts (which would be fragile and slow).
 */
class BacktrackingAllSolutionsSolverTest {

    // ===== Helpers =====

    // Validate one tour independently of Board state (length, uniqueness,
    // knight-adjacency, start, closure).
    private void assertValidTour(int rows, int cols, Position start, boolean closed, List<Position> tour) {
        // 1) Must visit all cells exactly once
        assertEquals(rows * cols, tour.size(), "Tour must visit all cells");

        // 2) Must start at the provided start
        assertEquals(start, tour.get(0), "Tour must start at the configured start position");

        // 3) All positions must be distinct
        Set<Position> set = new HashSet<>(tour);
        assertEquals(tour.size(), set.size(), "Tour must not revisit any cell");

        // 4) Every consecutive pair must be a knight move
        for (int i = 1; i < tour.size(); i++) {
            Position prev = tour.get(i - 1);
            Position curr = tour.get(i);
            assertTrue(prev.isAdjacent(curr), "Consecutive positions must be knight-adjacent: " + prev + " -> " + curr);
        }

        // 5) If closed tour, the last must connect back to start
        if (closed) {
            Position end = tour.get(tour.size() - 1);
            assertTrue(end.isAdjacent(start), "Closed tour: last move must connect back to start");
        }
    }

    private void assertAllToursValid(int rows, int cols, Position start, boolean closed, List<List<Position>> tours) {
        // Ensure there are no duplicate tours (basic check by exact sequence equality)
        Set<List<Position>> uniq = new HashSet<>(tours);
        assertEquals(tours.size(), uniq.size(), "All tours in the result set must be unique");

        for (List<Position> tour : tours) {
            assertValidTour(rows, cols, start, closed, tour);
        }
    }

    // ===== Tests =====

    @Test
    @Timeout(1)
    void testSolveAll_1x1_open_hasExactlyOneTour() {
        int rows = 1, cols = 1;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        AllToursSolver solver = new BacktrackingAllSolutionsSolver(board, start, /* closed= */false);
        List<List<Position>> all = solver.solveAll();

        // On a 1x1 board (open), there is exactly one tour: [(0,0)]
        assertEquals(1, all.size(), "Open 1x1 must have exactly one tour");
        assertAllToursValid(rows, cols, start, /* closed= */false, all);
        assertEquals(List.of(new Position(0, 0)), all.get(0), "The only tour on 1x1 must be [(0,0)]");
    }

    @Test
    @Timeout(1)
    void testSolveAll_1x1_closed_hasNoTours() {
        int rows = 1, cols = 1;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        AllToursSolver solver = new BacktrackingAllSolutionsSolver(board, start, /* closed= */true);
        List<List<Position>> all = solver.solveAll();

        // Closed 1x1 is impossible (no knight edge back to start)
        assertTrue(all.isEmpty(), "Closed 1x1 must have no tours");
    }

    @Test
    @Timeout(1)
    void testSolveAll_2x3_open_hasNoTours() {
        int rows = 2, cols = 3;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        AllToursSolver solver = new BacktrackingAllSolutionsSolver(board, start, /* closed= */false);
        List<List<Position>> all = solver.solveAll();

        // 2x3 is too small to admit a full knight's tour
        assertTrue(all.isEmpty(), "Open 2x3 must have no tours");
    }

    @Test
    @Timeout(1)
    void testSolveAll_2x3_closed_hasNoTours() {
        int rows = 2, cols = 3;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        AllToursSolver solver = new BacktrackingAllSolutionsSolver(board, start, /* closed= */true);
        List<List<Position>> all = solver.solveAll();

        // And closed 2x3 is also impossible
        assertTrue(all.isEmpty(), "Closed 2x3 must have no tours");
    }

    @Test
    @Timeout(1)
    void testSolve_matchesFirstOfSolveAll_on1x1_open() {
        int rows = 1, cols = 1;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);

        BacktrackingAllSolutionsSolver solver = new BacktrackingAllSolutionsSolver(board, start, /* closed= */false);

        List<Position> first = solver.solve(); // early-stop single solution
        List<List<Position>> all = solver.solveAll(); // enumerate all

        assertFalse(first.isEmpty(), "There must be a solution on 1x1 (open)");
        assertEquals(1, all.size(), "Open 1x1 has exactly one tour");
        assertEquals(all.get(0), first, "solve() must return the first tour of solveAll()");
    }
}
