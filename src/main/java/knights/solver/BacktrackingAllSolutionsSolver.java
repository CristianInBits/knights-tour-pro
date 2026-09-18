package knights.solver;

import knights.model.Board;
import knights.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Backtracking solver that can either:
 * Return the first tour found via solve()
 * Enumerate all tours via solveAll()
 * 
 * Thread-safety: not thread-safe; create a new instance per run.
 */
public final class BacktrackingAllSolutionsSolver implements AllToursSolver {

    private final Board board;
    private final Position start;
    private final boolean isClosed;

    // Reused across calls; cleared at the beginning of each solve/solveAll.
    private final List<Position> currentPath;
    private final List<List<Position>> allSolutions;

    public BacktrackingAllSolutionsSolver(Board board, Position start, boolean isClosed) {
        this.board = board;
        this.start = start;
        this.isClosed = isClosed;
        this.currentPath = new ArrayList<>(board.totalCells()); // preallocate path capacity
        this.allSolutions = new ArrayList<>();
    }

    /**
     * Finds and returns the first tour discovered, or an empty list if none exists.
     * This method stops the search immediately when a tour is found (no full
     * enumeration).
     */
    @Override
    public List<Position> solve() {
        board.reset();
        currentPath.clear();

        board.mark(start, 0);
        currentPath.add(start);

        List<Position> first = dfsFirst(start, 1);
        return first != null ? first : List.of();
    }

    /**
     * Enumerates and returns all tours from the start position.
     * WARNING: can be huge; consider streaming or limiting results instead.
     */
    @Override
    public List<List<Position>> solveAll() {
        board.reset();
        currentPath.clear();
        allSolutions.clear();

        board.mark(start, 0);
        currentPath.add(start);

        dfsAll(start, 1);
        // If you want to protect internal state from external mutation, return an
        // unmodifiable copy:
        // return List.copyOf(allSolutions);
        return allSolutions;
    }

    // ===== Internals =====

    /**
     * DFS that stops at the first tour and returns it; returns null if none exists.
     */
    private List<Position> dfsFirst(Position current, int step) {
        if (step == board.totalCells()) {
            if (!isClosed || current.isAdjacent(start)) {
                return new ArrayList<>(currentPath); // defensive copy
            }
            return null;
        }

        // Use board.legalMoves(current) to skip out-of-bounds candidates.
        for (Position next : board.legalMoves(current)) {
            if (board.isVisited(next))
                continue;

            board.mark(next, step);
            currentPath.add(next);

            List<Position> result = dfsFirst(next, step + 1);
            if (result != null)
                return result; // early stop on first solution

            currentPath.remove(currentPath.size() - 1);
            board.unmark(next);
        }
        return null;
    }

    /**
     * DFS that accumulates every tour into allSolutions.
     */
    private void dfsAll(Position current, int step) {
        if (step == board.totalCells()) {
            if (!isClosed || current.isAdjacent(start)) {
                allSolutions.add(new ArrayList<>(currentPath)); // defensive copy
            }
            return;
        }

        for (Position next : board.legalMoves(current)) {
            if (board.isVisited(next))
                continue;

            board.mark(next, step);
            currentPath.add(next);

            dfsAll(next, step + 1);

            currentPath.remove(currentPath.size() - 1);
            board.unmark(next);
        }
    }
}
