package knights.solver;

import java.util.concurrent.CancellationException;

/**
 * Cooperative cancellation shared by the solvers.
 *
 * A search has no safe point to be stopped from the outside: killing a thread mid-search
 * would leave a half-marked board behind. So the solvers check in at intervals instead,
 * and a caller stops one by interrupting its thread.
 *
 * The interrupt flag is read, never cleared, so the caller still sees it afterwards.
 */
final class Cancellation {

    /**
     * Nodes a solver may visit between checks. Reading the interrupt flag on every node
     * would sit right on the hot path; at this interval the cost disappears while a
     * cancelled search still gives up within microseconds.
     */
    static final int CHECK_INTERVAL = 4096;

    private Cancellation() {
        // Prevent instantiation
    }

    /** Throws if the calling thread has been interrupted. */
    static void abortIfInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Search cancelled");
        }
    }
}
