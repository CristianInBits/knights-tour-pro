package knights.model;

import java.util.ArrayList;
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

    /**
     * In-bounds knight moves for every square, indexed by row * cols + col.
     * Built once per board size and never modified, so copies share it.
     */
    private final List<List<Position>> neighbours;

    public static final int UNVISITED = -1;

    public Board(Board other) {
        this.rows = other.rows;
        this.cols = other.cols;
        this.path = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(other.path[i], 0, this.path[i], 0, cols);
        }
        this.steps = other.steps;
        // Same dimensions, and the table is immutable: sharing it keeps copies cheap,
        // which matters because the parallel solver copies a board per forked task.
        this.neighbours = other.neighbours;
    }

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.path = new int[rows][cols];
        this.steps = 0;
        this.neighbours = buildNeighbours(rows, cols);
        reset();
    }

    /**
     * Precomputes the in-bounds destinations of every square, keeping the order of
     * KnightMove.DX/DY so move ordering and its tie-breaks behave as before.
     */
    private static List<List<Position>> buildNeighbours(int rows, int cols) {
        List<List<Position>> table = new ArrayList<>(rows * cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                List<Position> moves = new ArrayList<>(KnightMove.TOTAL_MOVES);
                for (int i = 0; i < KnightMove.TOTAL_MOVES; i++) {
                    int nr = r + KnightMove.DX[i];
                    int nc = c + KnightMove.DY[i];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        moves.add(new Position(nr, nc));
                    }
                }
                table.add(List.copyOf(moves));
            }
        }
        return List.copyOf(table);
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

    /**
     * Returns the in-bounds knight moves from 'from', in KnightMove.DX/DY order.
     * This sits on the hot path of every solver, so the list is precomputed rather
     * than built per call; it is immutable and must not be modified by callers.
     * 'from' must be inside the board.
     */
    public List<Position> legalMoves(Position from) {
        return neighbours.get(from.row() * cols + from.col());
    }
}
