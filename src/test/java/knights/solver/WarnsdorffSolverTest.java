package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WarnsdorffSolverTest {

    @Test
    void testWarnsdorffFindsTourOn8x8Board() {
        int rows = 8, cols = 8;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        TourSolver solver = new WarnsdorffSolver(board, start, false);

        List<Position> solution = solver.solve();

        assertEquals(rows * cols, solution.size(), "Tour must visit all cells");
        assertFalse(solution.isEmpty(), "Solution should not be empty");
    }
}
