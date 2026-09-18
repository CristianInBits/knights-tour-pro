package knights.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Position.
 *
 * isAdjacent carries more weight than its size suggests: it is what every solver uses to
 * decide whether a closed tour actually closes, and what the solver tests use to check
 * that consecutive steps are legal. Equality matters too, because tours are checked for
 * repeated squares by putting positions in a set.
 */
class PositionTest {

    @Test
    void theEightKnightMovesAreAdjacent() {
        Position from = new Position(4, 4);
        int[][] deltas = { { -2, 1 }, { -1, 2 }, { 1, 2 }, { 2, 1 }, { 2, -1 }, { 1, -2 }, { -1, -2 }, { -2, -1 } };
        for (int[] d : deltas) {
            Position to = new Position(4 + d[0], 4 + d[1]);
            assertTrue(from.isAdjacent(to), from + " -> " + to + " should be adjacent");
        }
    }

    @Test
    void nearbySquaresThatAreNotKnightMovesAreNotAdjacent() {
        Position from = new Position(4, 4);
        assertFalse(from.isAdjacent(from), "a square is not adjacent to itself");
        assertFalse(from.isAdjacent(new Position(4, 5)), "one step sideways is a rook move");
        assertFalse(from.isAdjacent(new Position(5, 5)), "one step diagonally is a king move");
        assertFalse(from.isAdjacent(new Position(6, 6)), "two steps diagonally is a bishop move");
        assertFalse(from.isAdjacent(new Position(4, 6)), "two steps sideways is not a knight move");
        assertFalse(from.isAdjacent(new Position(7, 5)), "three by one is too far");
    }

    @Test
    void adjacencyWorksBothWays() {
        Position a = new Position(0, 0);
        Position b = new Position(1, 2);
        assertTrue(a.isAdjacent(b));
        assertTrue(b.isAdjacent(a));
    }

    @Test
    void adjacencyDoesNotCareAboutTheBoard() {
        // Positions know nothing about board size; clipping happens in Board.
        assertTrue(new Position(0, 0).isAdjacent(new Position(-1, -2)));
    }

    @Test
    void positionsWithTheSameCoordinatesAreEqual() {
        // Solver tests detect revisited squares with a HashSet, so this has to hold.
        assertEquals(new Position(2, 3), new Position(2, 3));
        assertEquals(new Position(2, 3).hashCode(), new Position(2, 3).hashCode());

        Set<Position> set = new HashSet<>();
        set.add(new Position(2, 3));
        set.add(new Position(2, 3));
        assertEquals(1, set.size());
    }

    @Test
    void rowAndColumnAreNotInterchangeable() {
        assertNotEquals(new Position(2, 3), new Position(3, 2));
    }

    @Test
    void toStringShowsRowThenColumn() {
        assertEquals("(2, 3)", new Position(2, 3).toString());
    }
}
