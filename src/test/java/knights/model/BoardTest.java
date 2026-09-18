package knights.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Board, with an emphasis on the precomputed neighbour table.
 *
 * The table replaced a computation that used to run on every call, so these tests pin
 * down the two properties that a future change could break without any solver test
 * noticing: the exact order of the moves, which decides both which tour a solver finds
 * first and how Warnsdorff breaks ties, and the row * cols + col indexing, which only a
 * non-square board can catch.
 */
class BoardTest {

    /** What legalMoves meant before the table existed; used as the reference. */
    private static List<Position> movesByDefinition(Board board, Position from) {
        List<Position> expected = new ArrayList<>();
        for (Position candidate : KnightMove.generateNextPositions(from)) {
            if (board.isInside(candidate)) {
                expected.add(candidate);
            }
        }
        return expected;
    }

    // ===== Neighbour table =====

    @Test
    void legalMovesFromTheMiddleKeepsTheKnightMoveOrder() {
        Board board = new Board(8, 8);
        // (4,4) is far enough from every edge that all eight moves stay on the board,
        // so this fixes the order itself rather than which moves survive clipping.
        assertEquals(
                List.of(new Position(2, 5), new Position(3, 6), new Position(5, 6), new Position(6, 5),
                        new Position(6, 3), new Position(5, 2), new Position(3, 2), new Position(2, 3)),
                board.legalMoves(new Position(4, 4)));
    }

    @Test
    void legalMovesFromACornerKeepsOnlyTheTwoOnTheBoard() {
        Board board = new Board(8, 8);
        assertEquals(List.of(new Position(1, 2), new Position(2, 1)),
                board.legalMoves(new Position(0, 0)));
    }

    @Test
    void legalMovesOnATinyBoardIsEmpty() {
        assertEquals(List.of(), new Board(1, 1).legalMoves(new Position(0, 0)));
        assertEquals(List.of(), new Board(2, 2).legalMoves(new Position(0, 0)));
    }

    @Test
    @Timeout(5)
    void legalMovesMatchesTheDefinitionOnEverySquare() {
        // Non-square sizes are the point here: a transposed index (col * rows + row)
        // still lands inside the table on a square board and would go unnoticed.
        int[][] sizes = { { 8, 8 }, { 5, 6 }, { 6, 5 }, { 3, 8 }, { 8, 3 }, { 1, 1 }, { 2, 3 }, { 1, 10 } };
        for (int[] size : sizes) {
            Board board = new Board(size[0], size[1]);
            for (int r = 0; r < size[0]; r++) {
                for (int c = 0; c < size[1]; c++) {
                    Position from = new Position(r, c);
                    assertEquals(movesByDefinition(board, from), board.legalMoves(from),
                            "legal moves differ on a " + size[0] + "x" + size[1] + " board at " + from);
                }
            }
        }
    }

    @Test
    void legalMovesCannotBeModifiedByCallers() {
        // The list is shared between every board of this size, so handing out a mutable
        // one would let a caller corrupt every other search.
        Board board = new Board(8, 8);
        List<Position> moves = board.legalMoves(new Position(4, 4));
        assertThrows(UnsupportedOperationException.class, () -> moves.add(new Position(0, 0)));
    }

    @Test
    void visitingSquaresDoesNotChangeTheLegalMoves() {
        // Legality is about the board edges; whether a square is taken is a separate question.
        Board board = new Board(8, 8);
        Position from = new Position(4, 4);
        List<Position> before = board.legalMoves(from);
        board.mark(new Position(2, 5), 0);
        assertEquals(before, board.legalMoves(from));
    }

    // ===== Copy constructor =====

    @Test
    void aCopyReportsTheSameLegalMoves() {
        Board original = new Board(5, 6);
        Board copy = new Board(original);
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 6; c++) {
                Position from = new Position(r, c);
                assertEquals(original.legalMoves(from), copy.legalMoves(from), "differ at " + from);
            }
        }
    }

    @Test
    void aCopyKeepsItsOwnVisitedSquares() {
        // The parallel solver forks a copy per task and relies on them not interfering.
        Board original = new Board(5, 5);
        original.mark(new Position(0, 0), 0);

        Board copy = new Board(original);
        assertTrue(copy.isVisited(new Position(0, 0)), "a copy starts from the original's state");

        copy.mark(new Position(1, 2), 1);
        assertFalse(original.isVisited(new Position(1, 2)), "writing to a copy must not touch the original");
        assertEquals(1, original.getSteps());
        assertEquals(2, copy.getSteps());
    }

    // ===== Marking =====

    @Test
    void markAndUnmarkTrackVisitedSquaresAndStepCount() {
        Board board = new Board(4, 4);
        Position p = new Position(1, 2);

        assertFalse(board.isVisited(p));
        assertEquals(0, board.getSteps());

        board.mark(p, 3);
        assertTrue(board.isVisited(p));
        assertEquals(1, board.getSteps());
        assertEquals(3, board.getPath()[1][2]);

        board.unmark(p);
        assertFalse(board.isVisited(p));
        assertEquals(0, board.getSteps());
        assertEquals(Board.UNVISITED, board.getPath()[1][2]);
    }

    @Test
    void markingStepZeroStillCountsAsVisited() {
        // The first move of a tour is step 0, which must not read as "unvisited".
        Board board = new Board(4, 4);
        board.mark(new Position(0, 0), 0);
        assertTrue(board.isVisited(new Position(0, 0)));
    }

    @Test
    void resetClearsEverySquare() {
        Board board = new Board(3, 4);
        board.mark(new Position(0, 0), 0);
        board.mark(new Position(1, 2), 1);

        board.reset();

        assertEquals(0, board.getSteps());
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                assertFalse(board.isVisited(new Position(r, c)), "still visited at (" + r + "," + c + ")");
            }
        }
    }

    // ===== Geometry =====

    @Test
    void isInsideRejectsEverythingOffTheBoard() {
        Board board = new Board(3, 5); // 3 rows, 5 columns
        assertTrue(board.isInside(new Position(0, 0)));
        assertTrue(board.isInside(new Position(2, 4)));

        assertFalse(board.isInside(new Position(-1, 0)));
        assertFalse(board.isInside(new Position(0, -1)));
        assertFalse(board.isInside(new Position(3, 0)), "row 3 is off a 3-row board");
        assertFalse(board.isInside(new Position(0, 5)), "column 5 is off a 5-column board");
        assertFalse(board.isInside(new Position(4, 2)), "rows and columns must not be swapped");
    }

    @Test
    void totalCellsIsRowsTimesColumns() {
        assertEquals(20, new Board(4, 5).totalCells());
        assertEquals(1, new Board(1, 1).totalCells());
    }
}
