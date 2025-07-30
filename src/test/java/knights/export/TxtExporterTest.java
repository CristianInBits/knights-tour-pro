package knights.export;

import knights.model.Position;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TxtExporterTest {

    @Test
    void testTxtExportCreatesFile() throws Exception {
        List<Position> tour1 = List.of(
                new Position(0, 0),
                new Position(2, 1),
                new Position(0, 2));

        List<Position> tour2 = List.of(
                new Position(1, 0),
                new Position(2, 2),
                new Position(0, 1));

        Map<String, Object> metadata = Map.of(
                "rows", 5,
                "cols", 5,
                "startRow", 0,
                "startCol", 0,
                "tourType", "open",
                "mode", "all",
                "strategy", "backtrack");

        File output = new File("output/test_tours.txt");
        output.getParentFile().mkdirs();

        TxtExporter.export(List.of(tour1, tour2), metadata, output.getPath());

        assertTrue(output.exists(), "The TXT file should be created");

        String content = Files.readString(output.toPath());
        assertTrue(content.contains("Tour Export"));
        assertTrue(content.contains("Solution #1:"));
        assertTrue(content.contains("(0,0)"));
        assertTrue(content.contains("Found Solutions: 2"));
    }
}
