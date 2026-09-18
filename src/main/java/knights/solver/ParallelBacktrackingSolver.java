package knights.solver;

import knights.model.Board;
import knights.model.KnightMove;
import knights.model.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Parallel backtracking solver (first-solution) using Fork/Join.
 *
 * Design notes:
 * - Each forked task owns a deep-copied Board and a copied path (no shared
 * mutation).
 * - Shared AtomicBoolean 'found' enables early-stop across the whole
 * computation tree.
 * - Work-first policy: compute one child locally and fork the rest for better
 * locality.
 * - Move ordering: Warnsdorff degree (ascending) with deterministic tie-break
 * by move index.
 *
 * Not thread-safe; create a new instance per run.
 */
public final class ParallelBacktrackingSolver implements TourSolver {

    private final Board board;
    private final Position start;
    private final boolean closed;
    private final int forkDepth;
    private final ForkJoinPool pool;

    public ParallelBacktrackingSolver(Board board, Position start, boolean closed, int forkDepth) {
        this(board, start, closed, forkDepth, null);
    }

    public ParallelBacktrackingSolver(Board board, Position start, boolean closed, int forkDepth, ForkJoinPool pool) {
        if (!board.isInside(start))
            throw new IllegalArgumentException("Start outside board");
        this.board = board;
        this.start = start;
        this.closed = closed;
        this.forkDepth = Math.max(0, forkDepth);
        this.pool = (pool != null) ? pool : ForkJoinPool.commonPool();
    }

    @Override
    public List<Position> solve() {
        // Use a shared 'found' flag so that tasks can short-circuit when a solution
        // appears elsewhere.
        AtomicBoolean found = new AtomicBoolean(false);

        List<Position> res = pool.invoke(
                new Task(board, start, List.of(), 0, closed, forkDepth, found));
        return (res != null) ? res : List.of();
    }

    // ===== Inner Task =====

    private static final class Task extends RecursiveTask<List<Position>> {
        private final Board b; // deep copy of board at fork boundary
        private final Position pos; // next position to place
        private final ArrayList<Position> path; // copy of the path so far
        private final int depth; // current fork depth
        private final boolean closed;
        private final int forkDepth;
        private final AtomicBoolean found; // shared early-stop flag

        Task(Board board, Position pos, List<Position> parentPath,
                int depth, boolean closed, int forkDepth, AtomicBoolean found) {
            // Deep copy of board state at fork time:
            this.b = new Board(board);
            // Copy path so far; we'll append 'pos' in compute():
            this.path = new ArrayList<>(parentPath);
            this.pos = pos;
            this.depth = depth;
            this.closed = closed;
            this.forkDepth = forkDepth;
            this.found = found;
        }

        @Override
        protected List<Position> compute() {
            if (found.get())
                return null; // global early-stop

            // Place current position
            path.add(pos);
            b.mark(pos, path.size() - 1);

            // Base case: full coverage
            if (path.size() == b.totalCells()) {
                // Closed vs open check
                if (!closed || path.get(path.size() - 1).isAdjacent(path.get(0))) {
                    found.compareAndSet(false, true); // signal others to stop ASAP
                    return new ArrayList<>(path); // defensive copy
                }
                return null;
            }

            // Generate candidate moves (unvisited, in-bounds) ordered by Warnsdorff degree
            List<Position> nextMoves = orderedMoves(b, path.get(path.size() - 1));

            if (nextMoves.isEmpty())
                return null;
            if (found.get())
                return null;

            // Forking policy
            if (depth < forkDepth && nextMoves.size() > 1) {
                // Work-first: compute the first locally, fork the rest
                List<Task> forks = new ArrayList<>(Math.max(0, nextMoves.size() - 1));

                for (int i = 1; i < nextMoves.size(); i++) {
                    Task t = new Task(b, nextMoves.get(i), path, depth + 1, closed, forkDepth, found);
                    t.fork();
                    forks.add(t);
                }
                // Compute the first child on this thread
                List<Position> res = new Task(b, nextMoves.get(0), path, depth + 1, closed, forkDepth, found).compute();
                if (res != null) {
                    found.compareAndSet(false, true);
                    // Join to satisfy FJP invariants; children should short-circuit quickly
                    for (Task t : forks)
                        t.join();
                    return res;
                }
                // Otherwise, join remaining forks and return the first non-null
                for (Task t : forks) {
                    List<Position> r = t.join();
                    if (r != null)
                        return r;
                }
                return null;
            } else {
                // Sequential fallback from this state
                return dfsSequential(b, path, closed, found);
            }
        }

        // Order candidate moves by onward degree (ascending) + deterministic tie-break
        // by move index
        private static List<Position> orderedMoves(Board board, Position from) {
            List<Position> moves = new ArrayList<>();
            for (Position p : board.legalMoves(from)) {
                if (!board.isVisited(p))
                    moves.add(p);
            }
            moves.sort(Comparator
                    .comparingInt((Position p) -> onwardDegree(board, p))
                    .thenComparingInt(p -> moveOrderIndex(from, p)));
            return moves;
        }

        // Count how many onward unvisited moves remain from 'p'
        private static int onwardDegree(Board board, Position p) {
            int c = 0;
            for (Position n : board.legalMoves(p)) {
                if (!board.isVisited(n))
                    c++;
            }
            return c;
        }

        // Deterministic tie-break by mapping (from -> to) delta to KnightMove offset
        // index (0..7)
        private static int moveOrderIndex(Position from, Position to) {
            int dx = to.row() - from.row();
            int dy = to.col() - from.col();
            for (int i = 0; i < KnightMove.TOTAL_MOVES; i++) {
                if (KnightMove.DX[i] == dx && KnightMove.DY[i] == dy)
                    return i;
            }
            return Integer.MAX_VALUE; // should not happen
        }

        // Sequential DFS using the same ordering; respects the global 'found' flag
        private static List<Position> dfsSequential(Board board, ArrayList<Position> path,
                boolean closed, AtomicBoolean found) {
            if (found.get())
                return null;

            if (path.size() == board.totalCells()) {
                if (!closed || path.get(path.size() - 1).isAdjacent(path.get(0))) {
                    found.compareAndSet(false, true);
                    return new ArrayList<>(path);
                }
                return null;
            }

            Position cur = path.get(path.size() - 1);
            List<Position> moves = orderedMoves(board, cur);

            for (Position nxt : moves) {
                if (found.get())
                    return null;

                int step = path.size();
                path.add(nxt);
                board.mark(nxt, step);

                List<Position> sol = dfsSequential(board, path, closed, found);
                if (sol != null)
                    return sol;

                board.unmark(nxt);
                path.remove(path.size() - 1);
            }
            return null;
        }
    }
}
