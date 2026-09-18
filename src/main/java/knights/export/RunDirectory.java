package knights.export;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gives each run its own folder under the output directory, named after the time it
 * started: {@code output/2026-09-18_231045/}.
 *
 * Exports used to land on fixed names, so a second run quietly overwrote the first. A
 * folder per run keeps every result and, since all the formats of one run live together,
 * makes it obvious which files belong to the same tour.
 *
 * Names are unique even when two runs start within the same second: the folder is created
 * with {@link Files#createDirectory}, which fails if the name is taken, and a numbered
 * suffix is tried until one works. That also makes it safe against another process writing
 * to the same output directory at the same moment.
 */
public final class RunDirectory {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    /** Enough attempts to clear any realistic pile-up within one second. */
    private static final int MAX_ATTEMPTS = 1000;

    private RunDirectory() {
        // Prevent instantiation
    }

    /**
     * Creates and returns a fresh folder under {@code base} for this run.
     *
     * @param base the output directory; created if it does not exist
     * @return the new folder, guaranteed not to have existed before this call
     * @throws IOException if the folder cannot be created
     */
    public static Path createUnder(Path base) throws IOException {
        return createUnder(base, LocalDateTime.now());
    }

    /** As {@link #createUnder(Path)}, with the timestamp supplied, for tests. */
    public static Path createUnder(Path base, LocalDateTime startedAt) throws IOException {
        Files.createDirectories(base);
        String stamp = startedAt.format(STAMP);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Path candidate = base.resolve(attempt == 1 ? stamp : stamp + "_" + attempt);
            try {
                return Files.createDirectory(candidate);
            } catch (FileAlreadyExistsException taken) {
                // Someone got there first — this run takes the next name along.
            }
        }
        throw new IOException("Could not find a free name for a run folder under " + base);
    }
}
