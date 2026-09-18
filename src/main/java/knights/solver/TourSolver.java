package knights.solver;

import knights.model.Position;
import java.util.List;
import java.util.concurrent.CancellationException;

public interface TourSolver {

    /**
     * Solves the Knight's Tour from a given starting position.
     *
     * <p>
     * A search can run for a long time, so implementations check whether the calling
     * thread has been interrupted and give up when it has. Callers that need to stop a
     * search interrupt the thread running it — typically through
     * {@code Future.cancel(true)} — and catch {@link CancellationException}.
     *
     * @return List of positions representing the tour in order,
     *         or empty list if no solution was found.
     * @throws CancellationException if the calling thread was interrupted before the
     *                               search finished. The thread's interrupt flag is left
     *                               set, so callers can see it too.
     */
    List<Position> solve();
}
