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

    public static List<Position> generateNextPositions(Position from) {
        return java.util.stream.IntStream.range(0, TOTAL_MOVES)
                .mapToObj(i -> new Position(from.row() + DX[i], from.col() + DY[i]))
                .toList();
    }
}
