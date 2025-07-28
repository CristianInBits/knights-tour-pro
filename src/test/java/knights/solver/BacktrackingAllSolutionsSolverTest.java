package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BacktrackingAllSolutionsSolverTest {

    @Test
    void testMultipleOpenTours5x5From00() {
        int rows = 5, cols = 5;
        Board board = new Board(rows, cols);
        Position start = new Position(0, 0);
        BacktrackingAllSolutionsSolver solver = new BacktrackingAllSolutionsSolver(board, start, false);

        List<List<Position>> allSolutions = solver.solveAll();

        assertFalse(allSolutions.isEmpty(), "Should find at least one tour");
        assertTrue(allSolutions.size() > 1, "Should find multiple tours on 5x5 board");
        assertEquals(rows * cols, allSolutions.get(0).size(), "Each tour must visit all cells");
    }
}
