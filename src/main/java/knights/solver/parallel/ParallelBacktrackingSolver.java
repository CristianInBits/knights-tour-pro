package knights.solver.parallel;

import knights.model.Board;
import knights.model.Position;
import knights.solver.TourSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

public class ParallelBacktrackingSolver implements TourSolver {

    private final Board board;
    private final Position start;
    private final boolean closed;
    private final int forkDepth = 2;

    public ParallelBacktrackingSolver(Board board, Position start, boolean closed) {
        this.board = board;
        this.start = start;
        this.closed = closed;
    }

    @Override
    public List<Position> solve() {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            List<List<Position>> all = pool.invoke(new Task(board, start, new ArrayList<>(), 0));
            return all.isEmpty() ? List.of() : all.get(0);
        } finally {
            pool.shutdown();
        }
    }

    public List<List<Position>> solveAll() {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            return pool.invoke(new Task(board, start, new ArrayList<>(), 0));
        } finally {
            pool.shutdown();
        }
    }

    private class Task extends RecursiveTask<List<List<Position>>> {
        private final Board board;
        private final Position pos;
        private final List<Position> path;
        private final int depth;

        Task(Board board, Position pos, List<Position> path, int depth) {
            this.board = new Board(board); // copia profunda
            this.pos = pos;
            this.path = new ArrayList<>(path);
            this.depth = depth;
        }

        @Override
        protected List<List<Position>> compute() {
            path.add(pos);
            board.mark(pos, path.size() - 1);

            if (path.size() == board.totalCells()) {
                if (!closed || path.get(0).isAdjacent(path.get(path.size() - 1))) {
                    return List.of(path);
                } else {
                    return List.of();
                }
            }

            List<Position> next = knights.model.KnightMove.generateNextPositions(pos).stream()
                    .filter(p -> board.isInside(p) && !board.isVisited(p))
                    .toList();

            List<List<Position>> results = new ArrayList<>();

            if (depth < forkDepth) {
                List<Task> tasks = new ArrayList<>();
                for (Position move : next) {
                    tasks.add(new Task(board, move, path, depth + 1));
                }
                invokeAll(tasks);
                for (Task t : tasks) {
                    results.addAll(t.join());
                }
            } else {
                for (Position move : next) {
                    Task t = new Task(board, move, path, depth + 1);
                    results.addAll(t.compute());
                }
            }

            return results;
        }
    }
}
