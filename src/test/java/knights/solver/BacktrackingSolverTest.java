package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BacktrackingSolverTest {

    // === Helpers ===
    private void assertKnightAdjacencyChain(List<Position> tour) {
        for (int i = 1; i < tour.size(); i++) {
            assertTrue(tour.get(i - 1).isAdjacent(tour.get(i)),
                    "Moves " + (i - 1) + "->" + i + " must be knight-adjacent: "
                            + tour.get(i - 1) + " -> " + tour.get(i));
        }
    }

    private void assertAllDistinct(List<Position> tour) {
        Set<Position> set = new HashSet<>(tour);
        assertEquals(tour.size(), set.size(), "Tour must not revisit any cell");
    }

    private void assertBoardConsistent(Board board, List<Position> tour) {
        int n = tour.size();
        assertEquals(n, board.getSteps(), "Board steps must match tour length");
        for (int idx = 0; idx < n; idx++) {
            Position p = tour.get(idx);
            int mark = board.getPath()[p.row()][p.col()];
            assertEquals(idx, mark, "Board mark must match tour order at " + p);
        }
    }

    // === Tests ===

    @Test
    @Timeout(3) // evita bloqueos si cambias heurísticas accidentalmente
    void testFindOpenTour5x5From00() {
        int rows = 5, cols = 5;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new BacktrackingSolver(board, start, false);

        List<Position> solution = solver.solve();

        assertFalse(solution.isEmpty(), "Solution should not be empty");
        assertEquals(rows * cols, solution.size(), "The tour should visit all cells");
        assertEquals(start, solution.get(0), "Tour must start at the given position");
        assertKnightAdjacencyChain(solution);
        assertAllDistinct(solution);
        assertBoardConsistent(board, solution);
    }

    @Test
    @Timeout(3)
    void testTourStartsAtGivenPosition() {
        int rows = 5, cols = 5;
        Board board = new Board(rows, cols);
        Position start = new Position(2, 2);
        TourSolver solver = new BacktrackingSolver(board, start, false);

        List<Position> solution = solver.solve();

        assertFalse(solution.isEmpty(), "Solution should not be empty");
        assertEquals(start, solution.get(0), "Tour must start at the given position");
    }

    @Test
    @Timeout(5)
    void testClosedTour6x6From00() {
        int rows = 6, cols = 6;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new BacktrackingSolver(board, start, true);

        List<Position> solution = solver.solve();

        assertFalse(solution.isEmpty(), "Solution should not be empty");
        assertEquals(rows * cols, solution.size(), "The tour should visit all cells");
        assertKnightAdjacencyChain(solution);
        assertAllDistinct(solution);
        assertBoardConsistent(board, solution);

        Position end = solution.get(solution.size() - 1);
        assertTrue(end.isAdjacent(start), "In a closed tour, last move must connect to start");
    }

    @Test
    @Timeout(1)
    void testNoTourExistsOn2x3() {
        int rows = 2, cols = 3;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new BacktrackingSolver(board, start, false);

        List<Position> solution = solver.solve();

        assertTrue(solution.isEmpty(), "No tour should exist on a 2x3 board");
    }
}
