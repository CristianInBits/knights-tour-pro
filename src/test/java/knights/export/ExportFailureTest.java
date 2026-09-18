package knights.export;

import knights.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that a failed export is reported instead of being swallowed.
 *
 * Both exporters used to catch every IOException, print it, and return normally, so the
 * caller could not tell a written file from a lost one. A long search whose results never
 * reached the disk would still look like a success.
 *
 * Each case makes a write genuinely impossible in a way that behaves the same on every
 * platform: a path whose parent is a regular file, and a path already taken by a
 * directory.
 */
class ExportFailureTest {

    private static final List<Position> TOUR = List.of(new Position(0, 0), new Position(1, 2));
    private static final Map<String, Object> META = Map.of("rows", 2, "cols", 3);

    /** A path whose parent is a file, so the parent directories cannot be created. */
    private static String underneathAFile(Path dir, String name) throws IOException {
        Path file = dir.resolve("not-a-directory");
        Files.writeString(file, "occupied");
        return file.resolve(name).toString();
    }

    /** A path already taken by a directory, so the file itself cannot be opened. */
    private static String onTopOfADirectory(Path dir, String name) throws IOException {
        Path inTheWay = dir.resolve(name);
        Files.createDirectory(inTheWay);
        return inTheWay.toString();
    }

    // ===== TXT =====

    @Test
    void txtReportsFailureWhenTheParentIsAFile(@TempDir Path dir) throws IOException {
        String target = underneathAFile(dir, "tour.txt");
        assertThrows(IOException.class,
                () -> new TxtExporter().exportSingle(TOUR, META, target),
                "a failed export must be reported, not logged and ignored");
    }

    @Test
    void txtReportsFailureWhenADirectoryIsInTheWay(@TempDir Path dir) throws IOException {
        String target = onTopOfADirectory(dir, "tour.txt");
        assertThrows(IOException.class,
                () -> new TxtExporter().exportMultiple(List.of(TOUR), META, target));
    }

    // ===== JSON =====

    @Test
    void jsonReportsFailureWhenTheParentIsAFile(@TempDir Path dir) throws IOException {
        String target = underneathAFile(dir, "tour.json");
        assertThrows(IOException.class,
                () -> new JsonExporter().exportSingle(TOUR, META, target),
                "a failed export must be reported, not logged and ignored");
    }

    @Test
    void jsonReportsFailureWhenADirectoryIsInTheWay(@TempDir Path dir) throws IOException {
        String target = onTopOfADirectory(dir, "tour.json");
        assertThrows(IOException.class,
                () -> new JsonExporter().exportMultiple(List.of(TOUR), META, target));
    }

    // ===== The other half: a good export still works =====

    @Test
    void aWritableTargetStillSucceeds(@TempDir Path dir) throws IOException {
        // Guards against "fixing" the tests by making every export throw.
        Path txt = dir.resolve("nested").resolve("tour.txt");
        Path json = dir.resolve("nested").resolve("tour.json");

        new TxtExporter().exportSingle(TOUR, META, txt.toString());
        new JsonExporter().exportSingle(TOUR, META, json.toString());

        assertTrue(Files.exists(txt), "the TXT file should have been written");
        assertTrue(Files.exists(json), "the JSON file should have been written");
        assertTrue(Files.size(txt) > 0);
        assertTrue(Files.size(json) > 0);
    }
}
