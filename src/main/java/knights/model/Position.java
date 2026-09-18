package knights.model;

/**
 * Represents a coordinate (row, col) on the board.
 */
public record Position(int row, int col) {

    public boolean isAdjacent(Position other) {
        int dx = Math.abs(this.row - other.row);
        int dy = Math.abs(this.col - other.col);
        return dx == 1 && dy == 2 || dx == 2 && dy == 1;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
