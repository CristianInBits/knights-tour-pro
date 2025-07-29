package knights.model;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the knight's tour board with move order tracking.
 */
public class Board {

    private final int rows;
    private final int cols;
    private final int[][] path;
    private int steps;

    public static final int UNVISITED = -1;

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.path = new int[rows][cols];
        this.steps = 0;
        reset();
    }

    public boolean isInside(Position p) {
        return p.row() >= 0 && p.row() < rows && p.col() >= 0 && p.col() < cols;
    }

    public boolean isVisited(Position p) {
        return path[p.row()][p.col()] != UNVISITED;
    }

    public void mark(Position p, int step) {
        path[p.row()][p.col()] = step;
        steps++;
    }

    public void unmark(Position p) {
        path[p.row()][p.col()] = UNVISITED;
        steps--;
    }

    public int getSteps() {
        return steps;
    }

    public int totalCells() {
        return rows * cols;
    }

    public int[][] getPath() {
        return path;
    }

    public void reset() {
        for (int[] row : path) {
            Arrays.fill(row, UNVISITED);
        }
        steps = 0;
    }

    public void print() {
        for (int[] row : path) {
            for (int cell : row) {
                System.out.printf("%3s ", cell == UNVISITED ? "." : cell);
            }
            System.out.println();
        }
    }

    public List<Position> legalMoves(Position from) {
        return KnightMove.generateNextPositions(from).stream()
                .filter(this::isInside)
                .toList();
    }
}
