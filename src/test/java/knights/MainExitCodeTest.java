package knights;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the exit code the command line reports for each kind of outcome.
 *
 * Every error path used to return normally, so a run that printed "Failed to export" and
 * one that wrote both files looked identical to whatever called it. A script had no way
 * to tell them apart.
 *
 * These go through Main.run, which returns the code instead of handing it to System.exit,
 * so they also end up covering the argument parsing itself.
 */
class MainExitCodeTest {

    /** Keeps the tests quiet and off the disk unless a case is specifically about files. */
    private static String[] quiet(String... args) {
        String[] full = new String[args.length + 2];
        System.arraycopy(args, 0, full, 0, args.length);
        full[args.length] = "--no-print";
        full[args.length + 1] = "--no-export";
        return full;
    }

    // ===== Success =====

    @Test
    @Timeout(20)
    void findingATourReportsSuccess() {
        assertEquals(Main.OK, Main.run(quiet("5", "5", "0", "0", "single", "open", "warnsdorff")));
    }

    @Test
    @Timeout(20)
    void enumeratingToursReportsSuccess() {
        assertEquals(Main.OK, Main.run(quiet("5", "5", "0", "0", "all", "open", "backtrack", "--limit", "0")));
    }

    // ===== A valid run with nothing to report =====

    @Test
    @Timeout(20)
    void aBoardWithNoTourIsNotAnError() {
        // 4x4 has no open tour at all: the run worked, the answer is simply "none".
        assertEquals(Main.NO_SOLUTION, Main.run(quiet("4", "4", "0", "0", "single", "open", "backtrack")));
    }

    @Test
    @Timeout(20)
    void warnsdorffGivingUpReportsNoSolution() {
        // The heuristic dead-ends on closed tours; that is a result, not a failure.
        assertEquals(Main.NO_SOLUTION, Main.run(quiet("6", "6", "0", "0", "single", "closed", "warnsdorff")));
    }

    // ===== Arguments that make no sense =====

    @Test
    @Timeout(10)
    void noArgumentsIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(new String[0]));
    }

    @Test
    @Timeout(10)
    void tooFewArgumentsIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(new String[] { "5", "5", "0", "0", "single" }));
    }

    @Test
    @Timeout(10)
    void aBoardSizeThatIsNotANumberIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(quiet("five", "5", "0", "0", "single", "open")));
    }

    @Test
    @Timeout(10)
    void anEmptyBoardIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(quiet("0", "5", "0", "0", "single", "open")));
    }

    @Test
    @Timeout(10)
    void startingOffTheBoardIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(quiet("5", "5", "9", "0", "single", "open")));
    }

    @Test
    @Timeout(10)
    void anUnknownModeIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(quiet("5", "5", "0", "0", "sometimes", "open")));
    }

    @Test
    @Timeout(10)
    void anUnknownStrategyIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(quiet("5", "5", "0", "0", "single", "open", "telepathy")));
    }

    @Test
    @Timeout(10)
    void anUnknownFlagIsAUsageError() {
        // The prefix check used to accept this as --out and write files elsewhere.
        assertEquals(Main.USAGE, Main.run(quiet("5", "5", "0", "0", "single", "open", "backtrack", "--outrageous")));
    }

    @Test
    @Timeout(10)
    void aFlagWithoutItsValueIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(new String[] { "5", "5", "0", "0", "single", "open", "backtrack", "--out" }));
    }

    @Test
    @Timeout(10)
    void aNonNumericFlagValueIsAUsageError() {
        assertEquals(Main.USAGE, Main.run(quiet("5", "5", "0", "0", "all", "open", "backtrack", "--limit", "many")));
    }

    @Test
    @Timeout(10)
    void askingWarnsdorffForEveryTourIsAUsageError() {
        // Only backtrack can enumerate; asking anything else is a contradiction.
        assertEquals(Main.USAGE, Main.run(quiet("5", "5", "0", "0", "all", "open", "warnsdorff")));
    }

    // ===== Files =====

    @Test
    @Timeout(20)
    void writingTheTourReportsSuccess(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("results");
        assertEquals(Main.OK,
                Main.run(new String[] { "5", "5", "0", "0", "single", "open", "warnsdorff",
                        "--no-print", "--out", out.toString() }));

        Path runDir = onlyRunFolder(out);
        assertTrue(Files.exists(runDir.resolve("tour.txt")));
        assertTrue(Files.exists(runDir.resolve("tour.json")));
        assertTrue(Files.exists(runDir.resolve("tour.svg")));
        assertTrue(Files.exists(runDir.resolve("tour.csv")));
    }

    @Test
    @Timeout(40)
    void aSecondRunDoesNotOverwriteTheFirst(@TempDir Path dir) throws IOException {
        // Results used to land on fixed names, so running twice lost the earlier one.
        Path out = dir.resolve("results");
        String[] args = { "5", "5", "0", "0", "single", "open", "warnsdorff",
                "--no-print", "--out", out.toString() };

        assertEquals(Main.OK, Main.run(args));
        assertEquals(Main.OK, Main.run(args));
        assertEquals(Main.OK, Main.run(args));

        List<Path> runs = runFolders(out);
        assertEquals(3, runs.size(), "each run should have left its own folder");
        for (Path run : runs) {
            assertTrue(Files.exists(run.resolve("tour.txt")), run.getFileName() + " lost its files");
            assertTrue(Files.size(run.resolve("tour.txt")) > 0);
        }
    }

    private static List<Path> runFolders(Path out) throws IOException {
        try (var entries = Files.list(out)) {
            return entries.filter(Files::isDirectory).sorted().toList();
        }
    }

    private static Path onlyRunFolder(Path out) throws IOException {
        List<Path> runs = runFolders(out);
        assertEquals(1, runs.size(), "expected exactly one run folder");
        return runs.get(0);
    }

    @Test
    @Timeout(10)
    void anUnusableOutputDirectoryIsAnIoError(@TempDir Path dir) throws IOException {
        Path blocked = dir.resolve("blocked");
        Files.writeString(blocked, "this is a file, not a directory");

        assertEquals(Main.IO_ERROR,
                Main.run(new String[] { "5", "5", "0", "0", "single", "open", "warnsdorff",
                        "--no-print", "--out", blocked.toString() }));
    }
}
