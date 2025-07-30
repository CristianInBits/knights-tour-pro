package knights.export;

import knights.model.Position;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonExporterTest {

    @Test
    void testJsonExportCreatesFile() throws Exception {
        List<Position> path = List.of(
                new Position(0, 0),
                new Position(2, 1),
                new Position(0, 2));

        Map<String, Object> metadata = Map.of(
                "rows", 5,
                "cols", 5,
                "startRow", 0,
                "startCol", 0,
                "tourType", "open",
                "mode", "single",
                "strategy", "backtrack");

        File output = new File("output/test_tour.json");
        output.getParentFile().mkdirs();

        JsonExporter.export(path, metadata, output.getPath());

        assertTrue(output.exists(), "The JSON file should be created");

        String content = Files.readString(output.toPath());
        assertTrue(content.contains("\"metadata\""));
        assertTrue(content.contains("\"path\""));
        assertTrue(content.contains("row"));
        assertTrue(content.contains("col"));
    }
}
