package knights.export;

import knights.model.Position;

import java.util.List;
import java.util.Map;

/**
 * Common interface for exporting tour results in various formats.
 */
public interface ResultExporter {

    /**
     * Exports a single tour.
     */
    void exportSingle(List<Position> path, Map<String, Object> metadata, String filePath);

    /**
     * Exports multiple tours.
     */
    void exportMultiple(List<List<Position>> paths, Map<String, Object> metadata, String filePath);
}
