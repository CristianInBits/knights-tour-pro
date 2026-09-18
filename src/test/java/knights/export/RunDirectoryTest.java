package knights.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the per-run output folder.
 *
 * The whole point is that a name is never handed out twice, so most of these are about
 * collisions: two runs in the same second, and two threads asking at once.
 */
class RunDirectoryTest {

    @Test
    void createsAFolderNamedAfterTheTime(@TempDir Path dir) throws IOException {
        LocalDateTime when = LocalDateTime.of(2026, 9, 18, 23, 10, 45);
        Path run = RunDirectory.createUnder(dir, when);

        assertTrue(Files.isDirectory(run));
        assertEquals("2026-09-18_231045", run.getFileName().toString());
        assertEquals(dir, run.getParent());
    }

    @Test
    void createsTheOutputDirectoryIfItIsMissing(@TempDir Path dir) throws IOException {
        Path base = dir.resolve("not").resolve("there").resolve("yet");
        Path run = RunDirectory.createUnder(base);
        assertTrue(Files.isDirectory(run));
    }

    @Test
    void twoRunsInTheSameSecondGetDifferentFolders(@TempDir Path dir) throws IOException {
        // The timestamp only goes down to seconds, so this is not a rare case: two quick
        // runs in a row hit it, and one would have overwritten the other.
        LocalDateTime when = LocalDateTime.of(2026, 9, 18, 23, 10, 45);

        Path first = RunDirectory.createUnder(dir, when);
        Path second = RunDirectory.createUnder(dir, when);
        Path third = RunDirectory.createUnder(dir, when);

        assertNotEquals(first, second);
        assertNotEquals(second, third);
        assertEquals("2026-09-18_231045", first.getFileName().toString());
        assertEquals("2026-09-18_231045_2", second.getFileName().toString());
        assertEquals("2026-09-18_231045_3", third.getFileName().toString());
        assertTrue(Files.isDirectory(first) && Files.isDirectory(second) && Files.isDirectory(third));
    }

    @Test
    void neverReturnsAFolderThatAlreadyHadSomethingInIt(@TempDir Path dir) throws IOException {
        LocalDateTime when = LocalDateTime.of(2026, 9, 18, 23, 10, 45);
        Path taken = Files.createDirectories(dir.resolve("2026-09-18_231045"));
        Files.writeString(taken.resolve("tour.txt"), "an earlier result");

        Path run = RunDirectory.createUnder(dir, when);

        assertNotEquals(taken, run);
        assertEquals(0, Files.list(run).count(), "a fresh run folder must be empty");
        assertEquals("an earlier result", Files.readString(taken.resolve("tour.txt")),
                "the earlier result must be left alone");
    }

    @Test
    @Timeout(30)
    void handsOutDistinctFoldersUnderConcurrentUse(@TempDir Path dir) throws Exception {
        // The folder is created with createDirectory, which fails if the name is taken,
        // so the check and the claim happen in one step rather than two.
        final int callers = 16;
        LocalDateTime when = LocalDateTime.of(2026, 9, 18, 23, 10, 45);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Path>> jobs = new java.util.ArrayList<>();
            for (int i = 0; i < callers; i++) {
                jobs.add(() -> RunDirectory.createUnder(dir, when));
            }

            Set<Path> handedOut = new HashSet<>();
            for (Future<Path> future : pool.invokeAll(jobs)) {
                assertTrue(handedOut.add(future.get(10, TimeUnit.SECONDS)),
                        "the same folder was handed to two callers");
            }
            assertEquals(callers, handedOut.size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void reportsFailureWhenTheOutputPathIsAFile(@TempDir Path dir) throws IOException {
        Path blocked = dir.resolve("blocked");
        Files.writeString(blocked, "this is a file, not a directory");
        assertThrows(IOException.class, () -> RunDirectory.createUnder(blocked));
    }

    @Test
    void foldersSortIntoChronologicalOrderByName(@TempDir Path dir) throws IOException {
        // The point of yyyy-MM-dd_HHmmss: sorting by name sorts by time.
        RunDirectory.createUnder(dir, LocalDateTime.of(2026, 9, 18, 9, 5, 3));
        RunDirectory.createUnder(dir, LocalDateTime.of(2026, 9, 18, 23, 10, 45));
        RunDirectory.createUnder(dir, LocalDateTime.of(2026, 1, 2, 12, 0, 0));

        try (var entries = Files.list(dir)) {
            List<String> names = entries.map(p -> p.getFileName().toString()).sorted().toList();
            assertEquals(List.of("2026-01-02_120000", "2026-09-18_090503", "2026-09-18_231045"), names);
        }
    }
}
