package knights.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the knight's move offsets.
 *
 * Their order is not an implementation detail: Board.legalMoves hands the moves out in
 * this order, plain backtracking therefore tries them in this order, and both Warnsdorff
 * solvers break ties on the index a move has here. Reordering DX/DY would change which
 * tour every solver returns, so the order is asserted explicitly.
 */
class KnightMoveTest {

    @Test
    void thereAreEightOffsetsAndTheArraysAgree() {
        assertEquals(8, KnightMove.TOTAL_MOVES);
        assertEquals(KnightMove.TOTAL_MOVES, KnightMove.DX.length);
        assertEquals(KnightMove.TOTAL_MOVES, KnightMove.DY.length);
    }

    @Test
    void everyOffsetIsARealKnightMove() {
        for (int i = 0; i < KnightMove.TOTAL_MOVES; i++) {
            int dx = Math.abs(KnightMove.DX[i]);
            int dy = Math.abs(KnightMove.DY[i]);
            assertTrue((dx == 1 && dy == 2) || (dx == 2 && dy == 1),
                    "offset " + i + " is (" + KnightMove.DX[i] + "," + KnightMove.DY[i] + ")");
        }
    }

    @Test
    void theEightOffsetsAreAllDifferent() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < KnightMove.TOTAL_MOVES; i++) {
            assertTrue(seen.add(KnightMove.DX[i] + "," + KnightMove.DY[i]),
                    "offset " + i + " is a duplicate");
        }
    }

    @Test
    void generateNextPositionsWalksTheOffsetsInOrder() {
        // Fixing the order here is what stops a reordering of DX/DY from silently
        // changing the tour every solver returns.
        assertEquals(
                List.of(new Position(2, 5), new Position(3, 6), new Position(5, 6), new Position(6, 5),
                        new Position(6, 3), new Position(5, 2), new Position(3, 2), new Position(2, 3)),
                KnightMove.generateNextPositions(new Position(4, 4)));
    }

    @Test
    void generateNextPositionsDoesNotCheckBounds() {
        // Clipping to the board is Board.legalMoves' job; this one always returns eight.
        List<Position> moves = KnightMove.generateNextPositions(new Position(0, 0));
        assertEquals(8, moves.size());
        assertTrue(moves.contains(new Position(-2, 1)), "negative coordinates are expected here");
    }

    @Test
    void everyGeneratedPositionIsAKnightMoveAway() {
        Position from = new Position(4, 4);
        for (Position to : KnightMove.generateNextPositions(from)) {
            assertTrue(from.isAdjacent(to), from + " -> " + to + " is not a knight move");
        }
    }
}
