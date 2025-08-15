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
    private final int forkDepth;

    public ParallelBacktrackingSolver(Board board, Position start, boolean closed, int forkDepth) {
        if (!board.isInside(start))
            throw new IllegalArgumentException("Start outside board");
        this.board = board;
        this.start = start;
        this.closed = closed;
        this.forkDepth = Math.max(0, forkDepth);
    }

    @Override
    public List<Position> solve() {
        return ForkJoinPool.commonPool().invoke(new Task(board, start, new ArrayList<>(), 0));
    }

    private final class Task extends RecursiveTask<List<Position>> {
        private final Board b; // copia local para esta rama paralela
        private final Position pos;
        private final List<Position> path; // copia local del camino
        private final int depth;

        Task(Board board, Position pos, List<Position> path, int depth) {
            this.b = new Board(board); // copia profunda SOLO al crear la Task
            this.pos = pos;
            this.path = new ArrayList<>(path);
            this.depth = depth;
        }

        @Override
        protected List<Position> compute() {
            path.add(pos);
            b.mark(pos, path.size() - 1);

            if (path.size() == b.totalCells()) {
                if (!closed || path.get(0).isAdjacent(path.get(path.size() - 1))) {
                    return new ArrayList<>(path); // devolver copia defensiva
                }
                return null;
            }

            // movimientos legales SIN heurística
            List<Position> next = knights.model.KnightMove.generateNextPositions(pos).stream()
                    .filter(p -> b.isInside(p) && !b.isVisited(p))
                    .toList();

            if (next.isEmpty())
                return null;

            if (depth < forkDepth) {
                // TRAMO PARALELO: forkeamos hijos
                List<Task> tasks = new ArrayList<>(next.size());
                for (Position move : next)
                    tasks.add(new Task(b, move, path, depth + 1));
                invokeAll(tasks);
                // devolvemos la PRIMERA solución que aparezca
                for (Task t : tasks) {
                    List<Position> sol = t.join();
                    if (sol != null)
                        return sol;
                }
                return null;
            } else {
                // TRAMO SECUENCIAL: backtracking in‑place SIN crear Tasks nuevas
                return dfsSequential(b, path, closed);
            }
        }

        private List<Position> dfsSequential(Board board, List<Position> path, boolean closed) {
            if (path.size() == board.totalCells()) {
                return (!closed || path.get(0).isAdjacent(path.get(path.size() - 1)))
                        ? new ArrayList<>(path)
                        : null;
            }
            Position cur = path.get(path.size() - 1);

            for (int i = 0; i < knights.model.KnightMove.TOTAL_MOVES; i++) {
                int r = cur.row() + knights.model.KnightMove.DX[i];
                int c = cur.col() + knights.model.KnightMove.DY[i];
                Position nxt = new Position(r, c);
                if (!board.isInside(nxt) || board.isVisited(nxt))
                    continue;

                int step = path.size();
                path.add(nxt);
                board.mark(nxt, step);

                List<Position> sol = dfsSequential(board, path, closed);
                if (sol != null)
                    return sol;

                board.unmark(nxt);
                path.remove(path.size() - 1);
            }
            return null;
        }
    }
}
