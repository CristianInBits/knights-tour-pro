package knights.solver;

import knights.model.Board;
import knights.model.Position;

import java.util.*;

/**
 * Solver based on Warnsdorff's heuristic: always move to the square with the
 * fewest onward moves.
 */
public class WarnsdorffSolver implements TourSolver {

    private final Board board;
    private final Position start;
    private final boolean closed;

    public WarnsdorffSolver(Board board, Position start, boolean closed) {
        this.board = board;
        this.start = start;
        this.closed = closed;
    }

    @Override
    public List<Position> solve() {
        List<Position> path = new ArrayList<>();
        board.reset();

        Position current = start;
        for (int step = 0; step < board.totalCells(); step++) {
            board.mark(current, step);
            path.add(current);
            current = selectNext(current);
            if (current == null && step != board.totalCells() - 1) {
                return Collections.emptyList();
            }
        }

        if (!closed || path.get(path.size() - 1).isAdjacent(start)) {
            return path;
        }

        return Collections.emptyList();
    }

    private Position selectNext(Position pos) {
        return board.legalMoves(pos).stream()
                .filter(p -> !board.isVisited(p))
                .min(Comparator.comparingInt(this::warnsdorffScore))
                .orElse(null);
    }

    private int warnsdorffScore(Position p) {
        return (int) board.legalMoves(p).stream()
                .filter(n -> !board.isVisited(n))
                .count();
    }
}
