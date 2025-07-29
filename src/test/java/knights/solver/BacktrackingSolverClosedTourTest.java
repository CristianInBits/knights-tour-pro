package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BacktrackingSolverClosedTourTest {

    @Test
    void testNoClosedTourExistsOn5x5Board() {
        int rows = 5, cols = 5;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new BacktrackingSolver(board, start, true); // closed = true

        List<Position> solution = solver.solve();

        assertTrue(solution.isEmpty(), "There should be no closed tour on a 5x5 board");
    }

    @Test
    void testClosedTourExistsOn6x6Board() {
        int rows = 6, cols = 6;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new BacktrackingSolver(board, start, true); // closed = true

        List<Position> solution = solver.solve();

        assertEquals(rows * cols, solution.size(), "The tour should visit all cells");
        assertFalse(solution.isEmpty(), "Solution should not be empty");

        Position end = solution.get(solution.size() - 1);
        assertTrue(end.isAdjacent(start), "In a closed tour, last move must connect to start");
    }
}
