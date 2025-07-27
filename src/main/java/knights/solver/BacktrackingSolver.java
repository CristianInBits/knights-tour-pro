package knights.solver;

import knights.model.Board;
import knights.model.Position;
import knights.model.KnightMove;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic backtracking implementation of the knight's tour solver.
 * Returns the first open tour found.
 */
public class BacktrackingSolver implements TourSolver {

    private final Board board;
    private final Position start;
    private final List<Position> path;

    public BacktrackingSolver(Board board, Position start) {
        this.board = board;
        this.start = start;
        this.path = new ArrayList<>();
    }

    @Override
    public List<Position> solve() {
        board.reset();
        path.clear();
        board.mark(start, 0);
        path.add(start);
        if (dfs(start, 1)) {
            return new ArrayList<>(path);
        } else {
            return List.of(); // empty list = no solution
        }
    }

    private boolean dfs(Position current, int step) {
        if (step == board.totalCells()) return true;

        for (Position next : KnightMove.generateNextPositions(current)) {
            if (board.isInside(next) && !board.isVisited(next)) {
                board.mark(next, step);
                path.add(next);
                if (dfs(next, step + 1)) 
                    return true;
                path.remove(path.size() - 1);
                board.unmark(next);
            }
        }

        return false;
    }
}
