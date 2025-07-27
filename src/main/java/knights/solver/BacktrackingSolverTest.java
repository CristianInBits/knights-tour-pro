package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BacktrackingSolverTest {

    @Test
    void testFindOpenTour5x5From00() {
        int rows = 5, cols = 5;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new BacktrackingSolver(board, start);

        List<Position> solution = solver.solve();

        assertEquals(rows * cols, solution.size(), "The tour should visit all cells");
        assertFalse(solution.isEmpty(), "Solution should not be empty");

        // Optional: print to check visually during dev
        board.print();
    }
}
