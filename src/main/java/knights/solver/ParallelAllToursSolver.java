package knights.solver;

import knights.model.Board;
import knights.model.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enumerates every tour, splitting the search tree across threads.
 *
 * This is a different proposition from finding one tour. There is no early exit — every
 * branch has to be walked whether or not a solution turns up — so the threads are simply
 * dividing a fixed amount of work, and the gain is bounded by how many cores there are
 * rather than by luck. That also makes it the case where parallelism behaves the way
 * people expect it to.
 *
 * Moves are tried in the plain board order that {@link BacktrackingAllSolutionsSolver}
 * uses, and the branches are joined in the order they were created rather than the order
 * they finish, so the solutions come out in exactly the same sequence as the sequential
 * enumeration. Anything that relied on that order keeps working.
 *
 * Be careful what you ask for: the tour count grows explosively with board size, and every
 * solution is held in memory.
 *
 * Not thread-safe; create a new instance per run.
 */
public final class ParallelAllToursSolver implements AllToursSolver {

    private final Board board;
    private final Position start;
    private final boolean closed;
    private final int forkDepth;
    private final ForkJoinPool pool;

    public ParallelAllToursSolver(Board board, Position start, boolean closed, int forkDepth) {
        this(board, start, closed, forkDepth, null);
    }

    public ParallelAllToursSolver(Board board, Position start, boolean closed, int forkDepth, ForkJoinPool pool) {
        if (!board.isInside(start)) {
            throw new IllegalArgumentException("Start outside board");
        }
        this.board = board;
        this.start = start;
        this.closed = closed;
        this.forkDepth = Math.max(0, forkDepth);
        this.pool = (pool != null) ? pool : ForkJoinPool.commonPool();
    }

    /**
     * Finding a single tour is a different problem, better served by the solver built for
     * it than by enumerating everything and taking the first.
     */
    @Override
    public List<Position> solve() {
        return new ParallelBacktrackingSolver(board, start, closed, forkDepth, pool).solve();
    }

    @Override
    public List<List<Position>> solveAll() {
        board.reset();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        ForkJoinTask<List<List<Position>>> task = pool.submit(
                new Task(board, start, List.of(), 0, closed, forkDepth, cancelled));
        try {
            return task.get();
        } catch (InterruptedException e) {
            cancelled.set(true);
            task.cancel(true);
            Thread.currentThread().interrupt();
            throw new CancellationException("Search cancelled");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Search failed", cause);
        }
    }

    // ===== Inner Task =====

    private static final class Task extends RecursiveTask<List<List<Position>>> {

        private final Board b;
        private final Position pos;
        private final ArrayList<Position> path;
        private final int depth;
        private final boolean closed;
        private final int forkDepth;
        private final AtomicBoolean cancelled;

        Task(Board board, Position pos, List<Position> parentPath,
                int depth, boolean closed, int forkDepth, AtomicBoolean cancelled) {
            this.b = new Board(board); // deep copy of the marks; the move table is shared
            this.path = new ArrayList<>(parentPath);
            this.pos = pos;
            this.depth = depth;
            this.closed = closed;
            this.forkDepth = forkDepth;
            this.cancelled = cancelled;
        }

        @Override
        protected List<List<Position>> compute() {
            if (stopped()) {
                return List.of();
            }

            path.add(pos);
            b.mark(pos, path.size() - 1);

            if (path.size() == b.totalCells()) {
                if (!closed || pos.isAdjacent(path.get(0))) {
                    return List.of(new ArrayList<>(path));
                }
                return List.of();
            }

            List<Position> moves = unvisitedMoves(b, pos);
            if (moves.isEmpty()) {
                return List.of();
            }

            if (depth < forkDepth && moves.size() > 1) {
                // One child is computed here and the rest are forked. They are joined in
                // the order they were created, not the order they finish, which is what
                // keeps the solutions in the sequential enumeration's order.
                List<Task> children = new ArrayList<>(moves.size());
                for (Position move : moves) {
                    children.add(new Task(b, move, path, depth + 1, closed, forkDepth, cancelled));
                }
                for (int i = 1; i < children.size(); i++) {
                    children.get(i).fork();
                }

                List<List<Position>> all = new ArrayList<>(children.get(0).compute());
                for (int i = 1; i < children.size(); i++) {
                    all.addAll(children.get(i).join());
                }
                return all;
            }

            List<List<Position>> found = new ArrayList<>();
            dfsAll(b, path, closed, cancelled, found);
            return found;
        }

        private boolean stopped() {
            return cancelled.get() || Thread.currentThread().isInterrupted();
        }

        /** In-bounds moves that are still free, in the board's own order. */
        private static List<Position> unvisitedMoves(Board board, Position from) {
            List<Position> moves = new ArrayList<>(8);
            for (Position p : board.legalMoves(from)) {
                if (!board.isVisited(p)) {
                    moves.add(p);
                }
            }
            return moves;
        }

        /**
         * Plain depth-first enumeration of everything below the current path, collecting
         * into 'found'. Mirrors BacktrackingAllSolutionsSolver so the two agree.
         */
        private static void dfsAll(Board board, ArrayList<Position> path, boolean closed,
                AtomicBoolean cancelled, List<List<Position>> found) {

            if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                return;
            }

            Position current = path.get(path.size() - 1);

            if (path.size() == board.totalCells()) {
                if (!closed || current.isAdjacent(path.get(0))) {
                    found.add(new ArrayList<>(path));
                }
                return;
            }

            for (Position next : board.legalMoves(current)) {
                if (board.isVisited(next)) {
                    continue;
                }
                int step = path.size();
                path.add(next);
                board.mark(next, step);

                dfsAll(board, path, closed, cancelled, found);

                board.unmark(next);
                path.remove(path.size() - 1);
            }
        }
    }
}
