package knights.export;

import knights.model.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TxtExporter:
 * - UTF-8 output
 * - Deterministic header and metadata presence
 * - Correct solution count and steps formatting
 * - Directory creation and null-safety
 */
class TxtExporterTest {

    @TempDir
    Path tempDir;

    private static String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    @Timeout(2)
    void exportSingle_writesExpectedContent() throws IOException {
        // Arrange
        ResultExporter exporter = new TxtExporter();
        List<Position> tour = List.of(
                new Position(0, 0),
                new Position(2, 1),
                new Position(4, 2));
        Map<String, Object> metadata = Map.of(
                "rows", 5,
                "cols", 5,
                "start", "(0,0)",
                "strategy", "warnsdorff",
                "closed", false);
        Path out = tempDir.resolve("txt/single_tour.txt");

        // Act
        exporter.exportSingle(tour, metadata, out.toString());

        // Assert
        assertTrue(Files.exists(out), "File must be created");
        String txt = readFile(out);

        // Header
        assertTrue(txt.contains("Tour Export"), "Header must be present");
        assertTrue(txt.contains("Found Solutions: 1"), "Must report 1 solution");

        // Metadata presence (order not asserted, just existence)
        assertTrue(txt.contains("rows: 5"), "rows metadata must be present");
        assertTrue(txt.contains("cols: 5"), "cols metadata must be present");
        assertTrue(txt.contains("start: (0,0)"), "start metadata must be present");
        assertTrue(txt.contains("strategy: warnsdorff"), "strategy metadata must be present");
        assertTrue(txt.contains("closed: false"), "closed metadata must be present");

        // Steps (robust match: don't rely on exact spacing)
        assertTrue(txt.contains("Solution #1:"), "Solution header must be present");
        assertTrue(txt.contains("(0,0)"), "First coord must be listed");
        assertTrue(txt.contains("(2,1)"), "Second coord must be listed");
        assertTrue(txt.contains("(4,2)"), "Third coord must be listed");
    }

    @Test
    @Timeout(2)
    void exportMultiple_writesAllSolutions() throws IOException {
        // Arrange
        ResultExporter exporter = new TxtExporter();
        List<Position> t1 = List.of(new Position(0, 0), new Position(2, 1));
        List<Position> t2 = List.of(new Position(1, 2), new Position(3, 3), new Position(1, 4));
        Path out = tempDir.resolve("txt/multiple_tours.txt");

        // Act
        exporter.exportMultiple(List.of(t1, t2), Map.of("rows", 5, "cols", 5), out.toString());

        // Assert
        String txt = readFile(out);
        assertTrue(txt.contains("Found Solutions: 2"), "Must report 2 solutions");
        assertTrue(txt.contains("Solution #1:"), "First solution header must be present");
        assertTrue(txt.contains("Solution #2:"), "Second solution header must be present");

        // Coordinates presence
        assertTrue(txt.contains("(0,0)"), "First tour coord must be present");
        assertTrue(txt.contains("(2,1)"), "First tour coord must be present");
        assertTrue(txt.contains("(1,2)"), "Second tour coord must be present");
        assertTrue(txt.contains("(3,3)"), "Second tour coord must be present");
        assertTrue(txt.contains("(1,4)"), "Second tour coord must be present");
    }

    @Test
    @Timeout(2)
    void export_handlesNullsAndCreatesDirectories() throws IOException {
        // Arrange
        ResultExporter exporter = new TxtExporter();
        Path nested = tempDir.resolve("deep/nested/out.txt");

        // Act (null paths/metadata should be tolerated and create empty scaffold)
        exporter.exportMultiple(null, null, nested.toString());

        // Assert
        assertTrue(Files.exists(nested), "Exporter must create parent directories and the file");
        String txt = readFile(nested);
        assertTrue(txt.contains("Found Solutions: 0"), "Empty paths should produce 0 solutions");
    }
}
