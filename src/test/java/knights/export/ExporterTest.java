package knights.export;

import knights.model.Position;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExporterTest {

    private static final List<Position> SAMPLE_PATH = List.of(
            new Position(0, 0),
            new Position(2, 1),
            new Position(0, 2));

    private static final Map<String, Object> SAMPLE_META = Map.of(
            "rows", 5,
            "cols", 5,
            "startRow", 0,
            "startCol", 0,
            "tourType", "open",
            "mode", "single",
            "strategy", "backtracking");

    private static final Map<String, Object> SAMPLE_META_MULTI = Map.of(
            "rows", 6,
            "cols", 6,
            "startRow", 0,
            "startCol", 0,
            "tourType", "closed",
            "mode", "all",
            "strategy", "backtracking");

    @Test
    void testJsonExporterSingle() throws Exception {
        File out = new File("output/test-single.json");
        File out2 = new File("output/test-single.txt");

        ResultExporter exporter = new JsonExporter();
        ResultExporter exporter2 = new TxtExporter();

        exporter.exportSingle(SAMPLE_PATH, SAMPLE_META, out.getPath());
        exporter2.exportSingle(SAMPLE_PATH, SAMPLE_META, out2.getPath());

        assertTrue(out.exists() && Files.size(out.toPath()) > 0);
        assertTrue(out2.exists() && Files.size(out.toPath()) > 0);
    }

    @Test
    void testTxtExporterMultiple() throws Exception {
        File out = new File("output/test-multi.txt");
        File out2 = new File("output/test-multi.json");

        ResultExporter exporter = new TxtExporter();
        ResultExporter exporter2 = new JsonExporter();

        exporter.exportMultiple(List.of(SAMPLE_PATH, SAMPLE_PATH), SAMPLE_META_MULTI, out.getPath());
        exporter2.exportMultiple(List.of(SAMPLE_PATH, SAMPLE_PATH), SAMPLE_META_MULTI, out2.getPath());

        assertTrue(out.exists() && Files.size(out.toPath()) > 0);
        assertTrue(out2.exists() && Files.size(out.toPath()) > 0);
    }
}
