package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that a long search can be called off.
 *
 * Each case starts a search that would otherwise run for a very long time, interrupts the
 * thread running it, and requires it to unwind quickly. The searches here are picked to be
 * genuinely slow — a closed tour on a board large enough that no solver finishes it in the
 * time these tests allow — so a passing test means cancellation did the work, not luck.
 */
class CancellationTest {

    /** How long a cancelled search may take to unwind before we call it a failure. */
    private static final long UNWIND_TIMEOUT_SECONDS = 5;

    /**
     * Runs a search on its own thread, waits until it has really started, interrupts it,
     * and returns whatever escaped — or fails if the search outlived the timeout.
     */
    private static Throwable cancelMidSearch(Runnable search) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch finished = new CountDownLatch(1);
            AtomicReference<Throwable> escaped = new AtomicReference<>();

            Future<?> running = executor.submit(() -> {
                started.countDown();
                try {
                    search.run();
                } catch (Throwable t) {
                    escaped.set(t);
                } finally {
                    finished.countDown();
                }
            });

            assertTrue(started.await(5, TimeUnit.SECONDS), "the search never started");
            // Give it a moment to get past the first few nodes, then pull the plug.
            Thread.sleep(150);
            running.cancel(true);

            // Waiting on the Future would be wrong: cancel() makes get() throw at once,
            // long before the search has actually unwound. Wait for the body to finish.
            assertTrue(finished.await(UNWIND_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the search was still running " + UNWIND_TIMEOUT_SECONDS + "s after being cancelled");
            return escaped.get();
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertCancelled(Throwable escaped) {
        assertNotNull(escaped, "the search finished instead of being cancelled; pick a slower case");
        assertInstanceOf(CancellationException.class, escaped,
                "expected a CancellationException but got: " + escaped);
    }

    // ===== Tests =====

    @Test
    @Timeout(20)
    void backtrackingStopsWhenInterrupted() throws Exception {
        // A closed tour on 8x8 from a corner: plain backtracking does not finish this
        // anywhere near the time budget of a test.
        assertCancelled(cancelMidSearch(
                () -> new BacktrackingSolver(new Board(8, 8), new Position(0, 0), true).solve()));
    }

    @Test
    @Timeout(20)
    void enumeratingEveryTourStopsWhenInterrupted() throws Exception {
        assertCancelled(cancelMidSearch(
                () -> new BacktrackingAllSolutionsSolver(new Board(6, 6), new Position(0, 0), false).solveAll()));
    }

    @Test
    @Timeout(20)
    void parallelBacktrackingStopsWhenInterrupted() throws Exception {
        // forkDepth 0 keeps this on the worker thread itself, so the sequential fallback
        // inside the parallel solver is what has to notice.
        // A private pool keeps abandoned tasks from draining through the common pool and
        // slowing down the rest of the suite.
        ForkJoinPool pool = new ForkJoinPool(2);
        try {
            assertCancelled(cancelMidSearch(
                    () -> new ParallelBacktrackingSolver(new Board(8, 8), new Position(0, 0), true, 0, pool).solve()));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(20)
    void parallelBacktrackingStopsWhenInterruptedWhileForking() throws Exception {
        // With real forking the search lives on pool threads that the interrupt never
        // reaches, so this exercises the shared cancellation flag instead.
        ForkJoinPool pool = new ForkJoinPool(4);
        try {
            assertCancelled(cancelMidSearch(
                    () -> new ParallelBacktrackingSolver(new Board(10, 10), new Position(0, 0), true, 4, pool).solve()));

            // solve() returning is not enough: interrupting the caller unblocks it whether
            // or not the workers noticed. If they kept searching, the pool stays busy and
            // the threads are lost — which is the whole failure this feature exists to fix.
            assertTrue(pool.awaitQuiescence(UNWIND_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the pool was still working after the search was cancelled");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(20)
    void theInterruptFlagSurvivesCancellation() throws Exception {
        // Callers further up the stack must still be able to see that they were interrupted.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<Boolean> stillInterrupted = new AtomicReference<>();
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch finished = new CountDownLatch(1);

            Future<?> running = executor.submit(() -> {
                started.countDown();
                try {
                    new BacktrackingSolver(new Board(8, 8), new Position(0, 0), true).solve();
                } catch (CancellationException expected) {
                    stillInterrupted.set(Thread.currentThread().isInterrupted());
                } finally {
                    finished.countDown();
                }
            });

            assertTrue(started.await(5, TimeUnit.SECONDS));
            Thread.sleep(150);
            running.cancel(true);
            assertTrue(finished.await(UNWIND_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "the search was still running after being cancelled");

            assertEquals(Boolean.TRUE, stillInterrupted.get(),
                    "the solver must leave the interrupt flag set");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(60)
    void repeatedCancellationsDoNotExhaustAFixedPool() throws Exception {
        // This is the failure the GUI actually showed. It runs searches on a small fixed
        // pool, and a Stop used to leave the search running for ever on its thread. After
        // as many Stops as there were threads, nothing could start again and the window
        // stopped responding. Cancel the same number of searches the pool has threads,
        // twice over, then check the pool can still do work.
        final int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads * 2; i++) {
                CountDownLatch started = new CountDownLatch(1);
                CountDownLatch finished = new CountDownLatch(1);

                Future<?> search = pool.submit(() -> {
                    started.countDown();
                    try {
                        new BacktrackingSolver(new Board(8, 8), new Position(0, 0), true).solve();
                    } catch (CancellationException expected) {
                        // The point of the test is that we get here at all.
                    } finally {
                        finished.countDown();
                    }
                });

                assertTrue(started.await(5, TimeUnit.SECONDS), "search " + i + " never started");
                Thread.sleep(100);
                search.cancel(true);
                assertTrue(finished.await(UNWIND_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "search " + i + " never released its thread");
            }

            Future<String> canary = pool.submit(() -> "alive");
            assertEquals("alive", canary.get(5, TimeUnit.SECONDS),
                    "the pool had no usable thread left after " + (threads * 2) + " cancellations");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(20)
    void anUninterruptedSearchIsUnaffected() throws Exception {
        // The checks must not fire on their own.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<List<Position>> result = executor.submit(
                    () -> new BacktrackingSolver(new Board(6, 6), new Position(0, 0), false).solve());
            List<Position> tour = result.get(15, TimeUnit.SECONDS);
            assertEquals(36, tour.size(), "a search nobody cancelled must return its tour");
        } finally {
            executor.shutdownNow();
        }
    }
}
