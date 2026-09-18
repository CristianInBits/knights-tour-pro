package knights.solver;

import knights.model.Board;
import knights.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * Tests for enumerating every tour in parallel.
 *
 * The bar here is higher than "the answers are valid": splitting the search must produce
 * the same set of tours as walking it in one thread, in the same order. Threads make
 * that easy to get subtly wrong — a branch counted twice, one dropped, or results joined
 * as they finish rather than as they were started — and a plain count would not catch it.
 */
class ParallelAllToursSolverTest {

    /** Flattens an enumeration into one string, so order counts as part of the answer. */
    private static String fingerprint(List<List<Position>> tours) {
        StringBuilder sb = new StringBuilder();
        for (List<Position> tour : tours) {
            for (Position p : tour) {
                sb.append(p.row()).append(',').append(p.col()).append(' ');
            }
            sb.append('|');
        }
        return sb.toString();
    }

    private static List<List<Position>> sequential(int rows, int cols, Position start, boolean closed) {
        return new BacktrackingAllSolutionsSolver(new Board(rows, cols), start, closed).solveAll();
    }

    private static List<List<Position>> parallel(int rows, int cols, Position start, boolean closed, int forkDepth) {
        return new ParallelAllToursSolver(new Board(rows, cols), start, closed, forkDepth).solveAll();
    }

    private static void assertSameEnumeration(int rows, int cols, Position start, boolean closed, int forkDepth) {
        List<List<Position>> expected = sequential(rows, cols, start, closed);
        List<List<Position>> actual = parallel(rows, cols, start, closed, forkDepth);
        assertEquals(expected.size(), actual.size(),
                "different number of tours on " + rows + "x" + cols);
        assertEquals(fingerprint(expected), fingerprint(actual),
                "the tours or their order differ on " + rows + "x" + cols);
    }

    // ===== Agreement with the sequential enumeration =====

    @Test
    @Timeout(60)
    void matchesTheSequentialEnumerationOnASquareBoard() {
        assertSameEnumeration(5, 5, new Position(0, 0), false, 3);
    }

    @Test
    @Timeout(60)
    void matchesOnRectangularBoardsBothWaysRound() {
        // Catches a row/column mix-up that a square board would hide.
        assertSameEnumeration(5, 6, new Position(0, 0), false, 3);
        assertSameEnumeration(6, 5, new Position(0, 0), false, 3);
    }

    @Test
    @Timeout(60)
    void matchesWhenStartingAwayFromTheCorner() {
        assertSameEnumeration(5, 5, new Position(2, 2), false, 3);
    }

    @Test
    @Timeout(60)
    void matchesForClosedTours() {
        assertSameEnumeration(5, 6, new Position(0, 0), true, 3);
    }

    @Test
    @Timeout(60)
    void matchesAtEveryForkDepth() {
        // Depth 0 never forks at all, so it doubles as a check that the split itself is
        // what changes and not the answer.
        for (int forkDepth : new int[] { 0, 1, 2, 4 }) {
            assertSameEnumeration(5, 5, new Position(0, 0), false, forkDepth);
        }
    }

    @Test
    @Timeout(30)
    void findsNothingOnABoardWithNoTours() {
        assertTrue(parallel(4, 4, new Position(0, 0), false, 3).isEmpty());
    }

    @Test
    @Timeout(30)
    void handlesTheSmallestBoards() {
        assertEquals(1, parallel(1, 1, new Position(0, 0), false, 2).size());
        assertTrue(parallel(1, 1, new Position(0, 0), true, 2).isEmpty());
        assertTrue(parallel(2, 3, new Position(0, 0), false, 2).isEmpty());
    }

    // ===== The tours themselves =====

    @Test
    @Timeout(60)
    void everyTourIsALegalCompleteWalk() {
        List<List<Position>> tours = parallel(5, 5, new Position(0, 0), false, 3);
        assertFalse(tours.isEmpty());

        for (List<Position> tour : tours) {
            assertEquals(25, tour.size(), "a tour must cover the board");
            assertEquals(new Position(0, 0), tour.get(0), "every tour starts where asked");
            assertEquals(tour.size(), new HashSet<>(tour).size(), "no square twice");
            for (int i = 1; i < tour.size(); i++) {
                assertTrue(tour.get(i - 1).isAdjacent(tour.get(i)),
                        "consecutive squares must be a knight's move apart");
            }
        }
    }

    @Test
    @Timeout(60)
    void noTourIsReturnedTwice() {
        // A branch explored by two tasks would show up here and nowhere else.
        List<List<Position>> tours = parallel(5, 6, new Position(0, 0), false, 3);
        Set<String> seen = new HashSet<>();
        for (List<Position> tour : tours) {
            assertTrue(seen.add(fingerprint(List.of(tour))), "the same tour came back more than once");
        }
        assertEquals(tours.size(), seen.size());
    }

    // ===== Plumbing =====

    @Test
    @Timeout(30)
    void rejectsAStartOutsideTheBoard() {
        assertThrows(IllegalArgumentException.class,
                () -> new ParallelAllToursSolver(new Board(5, 5), new Position(5, 0), false, 2));
    }

    @Test
    @Timeout(60)
    void acceptsItsOwnThreadPool() {
        ForkJoinPool pool = new ForkJoinPool(2);
        try {
            List<List<Position>> tours = new ParallelAllToursSolver(
                    new Board(5, 5), new Position(0, 0), false, 3, pool).solveAll();
            assertEquals(304, tours.size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(60)
    void solveStillReturnsASingleTour() {
        List<Position> tour = new ParallelAllToursSolver(
                new Board(6, 6), new Position(0, 0), false, 3).solve();
        assertEquals(36, tour.size());
    }

    @Test
    @Timeout(60)
    void theResultCanBeModifiedWithoutAffectingTheSolver() {
        // The sequential enumerator hands back its own list; this one must not.
        ParallelAllToursSolver solver = new ParallelAllToursSolver(
                new Board(5, 5), new Position(0, 0), false, 2);
        List<List<Position>> first = solver.solveAll();
        int count = first.size();
        first.clear();
        assertEquals(count, solver.solveAll().size(), "clearing the result must not affect a later run");
    }

    @Test
    @Timeout(60)
    void stopsWhenInterrupted() throws Exception {
        // 6x6 has millions of tours; nothing finishes it inside this test.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch finished = new CountDownLatch(1);
            AtomicReference<Throwable> escaped = new AtomicReference<>();
            ForkJoinPool pool = new ForkJoinPool(4);

            Future<?> search = executor.submit(() -> {
                started.countDown();
                try {
                    new ParallelAllToursSolver(new Board(6, 6), new Position(0, 0), false, 3, pool).solveAll();
                } catch (Throwable t) {
                    escaped.set(t);
                } finally {
                    finished.countDown();
                }
            });

            assertTrue(started.await(5, TimeUnit.SECONDS));
            Thread.sleep(300);
            search.cancel(true);

            assertTrue(finished.await(15, TimeUnit.SECONDS), "the enumeration ignored the interrupt");
            assertInstanceOf(CancellationException.class, escaped.get(),
                    "expected a CancellationException but got: " + escaped.get());
            assertTrue(pool.awaitQuiescence(15, TimeUnit.SECONDS), "the pool kept working after cancellation");
            pool.shutdownNow();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(60)
    void anUninterruptedEnumerationIsUnaffected() {
        // The cancellation checks must not fire on their own.
        assertEquals(304, new ArrayList<>(parallel(5, 5, new Position(0, 0), false, 3)).size());
    }
}
