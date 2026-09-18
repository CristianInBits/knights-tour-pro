package knights.model;

import java.util.List;

/**
 * Static utility for knight's possible move offsets.
 */
public final class KnightMove {

    public static final int[] DX = { -2, -1, 1, 2, 2, 1, -1, -2 };
    public static final int[] DY = { 1, 2, 2, 1, -1, -2, -2, -1 };

    public static final int TOTAL_MOVES = DX.length;

    private KnightMove() {
        // Prevent instantiation
    }

    /**
     * All eight knight destinations from 'from', bounds unchecked.
     * Solvers use Board.legalMoves instead, which precomputes the in-bounds ones.
     */
    public static List<Position> generateNextPositions(Position from) {
        Position[] moves = new Position[TOTAL_MOVES];
        for (int i = 0; i < TOTAL_MOVES; i++) {
            moves[i] = new Position(from.row() + DX[i], from.col() + DY[i]);
        }
        return List.of(moves);
    }
}
