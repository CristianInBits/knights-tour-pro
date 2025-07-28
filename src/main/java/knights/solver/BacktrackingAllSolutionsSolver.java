package knights.solver;

import knights.model.Board;
import knights.model.Position;
import knights.model.KnightMove;

import java.util.ArrayList;
import java.util.List;

/**
 * Backtracking solver that accumulates all possible knight's tours.
 */
public class BacktrackingAllSolutionsSolver implements TourSolver {

    private final Board board;
    private final Position start;
    private final boolean isClosed;

    private final List<List<Position>> allSolutions = new ArrayList<>();
    private final List<Position> currentPath = new ArrayList<>();

    public BacktrackingAllSolutionsSolver(Board board, Position start, boolean isClosed) {
        this.board = board;
        this.start = start;
        this.isClosed = isClosed;
    }

    @Override
    public List<Position> solve() {
        board.reset();
        currentPath.clear();
        allSolutions.clear();

        board.mark(start, 0);
        currentPath.add(start);
        dfs(start, 1);
        return allSolutions.isEmpty() ? List.of() : allSolutions.get(0);
    }

    public List<List<Position>> solveAll() {
        solve(); // same logic
        return allSolutions;
    }

    private void dfs(Position current, int step) {
        if (step == board.totalCells()) {
            if (!isClosed || current.isAdjacent(start)) {
                allSolutions.add(new ArrayList<>(currentPath));
            }
            return;
        }

        for (Position next : KnightMove.generateNextPositions(current)) {
            if (board.isInside(next) && !board.isVisited(next)) {
                board.mark(next, step);
                currentPath.add(next);
                dfs(next, step + 1);
                currentPath.remove(currentPath.size() - 1);
                board.unmark(next);
            }
        }
    }
}
