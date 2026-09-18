package knights.export;

import knights.model.Position;
import java.util.List;
import java.util.Map;

/**
 * Exports Knight's Tour results to a target file.
 * Implementations should document the output format and encoding (UTF-8
 * recommended).
 * Methods should be null-safe for metadata and tolerate empty path lists.
 */
public interface ResultExporter {
    /**
     * Exports a single tour.
     * 
     * @param path     tour positions in order; may be empty
     * @param metadata additional info (board size, start, strategy, timestamp,
     *                 etc.); can be null
     * @param filePath target file path (string form)
     */
    void exportSingle(List<Position> path, Map<String, Object> metadata, String filePath);

    /**
     * Exports multiple tours.
     * 
     * @param paths    list of tours; may be empty
     * @param metadata additional info; can be null
     * @param filePath target file path (string form)
     */
    void exportMultiple(List<List<Position>> paths, Map<String, Object> metadata, String filePath);
}
