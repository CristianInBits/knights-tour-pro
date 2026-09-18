package knights.export;

import knights.model.Board;
import knights.model.Position;
import knights.solver.WarnsdorffSolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CSV export.
 *
 * A CSV is only worth anything if another program can load it, so these check the shape
 * of the file — header, row count, line endings — and then read the numbers back and
 * compare them against the tour they came from.
 */
class CsvExporterTest {

    private static final Map<String, Object> META = Map.of("rows", 5, "cols", 5);

    private static List<Position> tourOf(int size) {
        List<Position> tour = new WarnsdorffSolver(new Board(size, size), new Position(0, 0), false).solve();
        assertFalse(tour.isEmpty(), "the test needs a real tour to write");
        return tour;
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /** Data rows, header dropped, empty trailing line ignored. */
    private static List<String> dataRows(Path file) throws IOException {
        String[] lines = read(file).split("\r\n");
        return List.of(lines).subList(1, lines.length);
    }

    // ===== Tests =====

    @Test
    void startsWithTheHeader(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tour.csv");
        new CsvExporter().exportSingle(tourOf(5), META, file.toString());
        assertTrue(read(file).startsWith("solution,step,row,col\r\n"));
    }

    @Test
    void writesOneRowPerMove(@TempDir Path dir) throws IOException {
        List<Position> tour = tourOf(5);
        Path file = dir.resolve("tour.csv");
        new CsvExporter().exportSingle(tour, META, file.toString());
        assertEquals(tour.size(), dataRows(file).size());
    }

    @Test
    void theNumbersMatchTheTour(@TempDir Path dir) throws IOException {
        List<Position> tour = tourOf(5);
        Path file = dir.resolve("tour.csv");
        new CsvExporter().exportSingle(tour, META, file.toString());

        List<String> rows = dataRows(file);
        for (int i = 0; i < tour.size(); i++) {
            String[] cells = rows.get(i).split(",");
            assertEquals(4, cells.length, "row " + i + " should have four columns");
            assertEquals("1", cells[0], "single mode always reports solution 1");
            assertEquals(String.valueOf(i + 1), cells[1], "steps are numbered from 1");
            assertEquals(String.valueOf(tour.get(i).row()), cells[2]);
            assertEquals(String.valueOf(tour.get(i).col()), cells[3]);
        }
    }

    @Test
    void numbersEachSolutionInTurn(@TempDir Path dir) throws IOException {
        List<Position> tour = tourOf(5);
        Path file = dir.resolve("tours.csv");
        new CsvExporter().exportMultiple(List.of(tour, tour, tour), META, file.toString());

        List<String> rows = dataRows(file);
        assertEquals(3 * tour.size(), rows.size());
        assertEquals("1", rows.get(0).split(",")[0]);
        assertEquals("2", rows.get(tour.size()).split(",")[0]);
        assertEquals("3", rows.get(2 * tour.size()).split(",")[0]);
    }

    @Test
    void usesCrlfLineEndings(@TempDir Path dir) throws IOException {
        // RFC 4180, and what Excel expects.
        Path file = dir.resolve("tour.csv");
        List<Position> tour = tourOf(5);
        new CsvExporter().exportSingle(tour, META, file.toString());

        String content = read(file);
        assertEquals(tour.size() + 1, content.split("\r\n").length);
        assertFalse(content.replace("\r\n", "").contains("\n"), "no bare newlines should remain");
    }

    @Test
    void anEmptyResultStillWritesTheHeader(@TempDir Path dir) throws IOException {
        // A loadable file with no rows beats an empty one that breaks the reader.
        Path file = dir.resolve("tours.csv");
        new CsvExporter().exportMultiple(List.of(), META, file.toString());
        assertEquals("solution,step,row,col\r\n", read(file));
    }

    @Test
    void worksWithoutMetadata(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("tour.csv");
        new CsvExporter().exportSingle(tourOf(5), null, file.toString());
        assertEquals(tourOf(5).size(), dataRows(file).size());
    }

    @Test
    void createsMissingDirectories(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("nested").resolve("deeper").resolve("tour.csv");
        new CsvExporter().exportSingle(tourOf(5), META, file.toString());
        assertTrue(Files.exists(file));
    }

    @Test
    void reportsAFailedWrite(@TempDir Path dir) throws IOException {
        Path blocked = dir.resolve("tour.csv");
        Files.createDirectory(blocked);
        assertThrows(IOException.class,
                () -> new CsvExporter().exportSingle(tourOf(5), META, blocked.toString()));
    }
}
