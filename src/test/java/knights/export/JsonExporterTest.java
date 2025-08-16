package knights.export;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
 * Tests for JsonExporter:
 * - UTF-8 output and valid JSON
 * - metadata object presence and key-values
 * - paths array structure and coordinates
 * - directory creation and null-safety
 */
class JsonExporterTest {

    @TempDir
    Path tempDir;

    private static String readFile(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static JsonObject parseJson(String s) {
        return new Gson().fromJson(s, JsonObject.class);
    }

    @Test
    @Timeout(2)
    void exportSingle_validJsonStructure() throws IOException {
        // Arrange
        ResultExporter exporter = new JsonExporter();
        List<Position> tour = List.of(
                new Position(0, 0),
                new Position(2, 1),
                new Position(4, 2));
        Map<String, Object> metadata = Map.of(
                "rows", 5,
                "cols", 5,
                "start", "(0,0)",
                "strategy", "backtracking",
                "closed", false);
        Path out = tempDir.resolve("json/single.json");

        // Act
        exporter.exportSingle(tour, metadata, out.toString());

        // Assert (file exists and is valid JSON with expected fields)
        assertTrue(Files.exists(out), "File must be created");
        JsonObject root = parseJson(readFile(out));

        assertTrue(root.has("metadata"), "metadata field must exist");
        assertTrue(root.has("paths"), "paths field must exist");

        JsonObject meta = root.getAsJsonObject("metadata");
        assertEquals(5, meta.get("rows").getAsInt());
        assertEquals(5, meta.get("cols").getAsInt());
        assertEquals("(0,0)", meta.get("start").getAsString());
        assertEquals("backtracking", meta.get("strategy").getAsString());
        assertFalse(meta.get("closed").getAsBoolean());

        JsonArray paths = root.getAsJsonArray("paths");
        assertEquals(1, paths.size(), "Single export must produce one tour entry");

        JsonArray steps = paths.get(0).getAsJsonArray();
        assertEquals(3, steps.size(), "Tour must have 3 positions");

        JsonObject p0 = steps.get(0).getAsJsonObject();
        assertEquals(0, p0.get("row").getAsInt());
        assertEquals(0, p0.get("col").getAsInt());

        JsonObject p2 = steps.get(2).getAsJsonObject();
        assertEquals(4, p2.get("row").getAsInt());
        assertEquals(2, p2.get("col").getAsInt());
    }

    @Test
    @Timeout(2)
    void exportMultiple_validatesArrayOfTours() throws IOException {
        // Arrange
        ResultExporter exporter = new JsonExporter();
        List<Position> t1 = List.of(new Position(0, 0), new Position(2, 1));
        List<Position> t2 = List.of(new Position(1, 2), new Position(3, 3), new Position(1, 4));
        Path out = tempDir.resolve("json/multiple.json");

        // Act
        exporter.exportMultiple(List.of(t1, t2), Map.of("rows", 5, "cols", 5), out.toString());

        // Assert
        JsonObject root = parseJson(readFile(out));
        JsonArray paths = root.getAsJsonArray("paths");
        assertEquals(2, paths.size(), "Must contain two tours");

        JsonArray first = paths.get(0).getAsJsonArray();
        assertEquals(2, first.size(), "First tour length");
        assertEquals(0, first.get(0).getAsJsonObject().get("row").getAsInt());
        assertEquals(0, first.get(0).getAsJsonObject().get("col").getAsInt());

        JsonArray second = paths.get(1).getAsJsonArray();
        assertEquals(3, second.size(), "Second tour length");
        assertEquals(1, second.get(2).getAsJsonObject().get("row").getAsInt());
        assertEquals(4, second.get(2).getAsJsonObject().get("col").getAsInt());
    }

    @Test
    @Timeout(2)
    void export_handlesNullsAndCreatesDirectories() throws IOException {
        // Arrange
        ResultExporter exporter = new JsonExporter();
        Path nested = tempDir.resolve("deep/tree/out.json");

        // Act
        exporter.exportMultiple(null, null, nested.toString());

        // Assert
        assertTrue(Files.exists(nested), "Exporter must create parent directories and the file");
        JsonObject root = parseJson(readFile(nested));

        assertTrue(root.has("metadata"), "metadata must exist even if empty");
        assertTrue(root.has("paths"), "paths must exist even if empty");
        assertEquals(0, root.getAsJsonArray("paths").size(), "Empty paths should produce empty array");
    }
}
