package knights.solver;

import knights.model.Board;
import knights.model.KnightMove;
import knights.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Solver based on Warnsdorff's heuristic: always move to the square with the
 * fewest onward moves.
 * Notes:
 * - Deterministic tie-break: when multiple candidates share the same degree,
 * we pick the one that respects the base move-order (KnightMove.DX/DY index).
 * - Not thread-safe; create a new instance per run.
 */
public final class WarnsdorffSolver implements TourSolver {

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
        final int total = board.totalCells();
        final List<Position> path = new ArrayList<>(total);

        board.reset();

        Position current = start;
        for (int step = 0; step < total; step++) {
            board.mark(current, step);
            path.add(current);

            // If we just placed the last step, break the loop and validate closure (if
            // required).
            if (step == total - 1)
                break;

            Position next = selectNextDeterministic(current);
            if (next == null) {
                // Dead-end before covering all cells -> no tour under this heuristic.
                return List.of();
            }
            current = next;
        }

        // Open vs closed validation
        if (!closed || path.get(path.size() - 1).isAdjacent(start)) {
            return path;
        }
        return List.of();
    }

    /**
     * Pick the next move using Warnsdorff's degree (ascending), with a
     * deterministic tie-break
     * based on the underlying KnightMove offset order (0..7).
     */
    private Position selectNextDeterministic(Position from) {
        Position best = null;
        int bestDegree = Integer.MAX_VALUE;
        int bestTie = Integer.MAX_VALUE; // lower is better (offset index 0..7)

        // Iterate legal (in-bounds) moves; skip visited; evaluate degree once per
        // candidate.
        for (Position cand : board.legalMoves(from)) {
            if (board.isVisited(cand))
                continue;

            int degree = onwardDegree(cand);
            int tie = moveOrderIndex(from, cand);

            // Primary: degree; Secondary: tie-break by base move order
            if (degree < bestDegree || (degree == bestDegree && tie < bestTie)) {
                best = cand;
                bestDegree = degree;
                bestTie = tie;
            }
        }
        return best;
        // If no unvisited legal moves exist, returns null (dead-end).
    }

    /**
     * Count how many onward moves remain from 'p' that are not yet visited.
     */
    private int onwardDegree(Position p) {
        int count = 0;
        for (Position n : board.legalMoves(p)) {
            if (!board.isVisited(n))
                count++;
        }
        return count;
    }

    /**
     * Deterministic tie-break: map the (from -> to) delta to the KnightMove offset
     * index (0..7).
     * Lower index wins to keep a stable selection order across runs/jvms.
     */
    private int moveOrderIndex(Position from, Position to) {
        int dx = to.row() - from.row();
        int dy = to.col() - from.col();
        for (int i = 0; i < KnightMove.TOTAL_MOVES; i++) {
            if (KnightMove.DX[i] == dx && KnightMove.DY[i] == dy)
                return i;
        }
        // Should not happen if 'to' came from legalMoves(from), but return a large
        // number just in case.
        return Integer.MAX_VALUE;
    }
}
