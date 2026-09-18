package knights.export;

import knights.model.Position;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Plain-text exporter for Knight's Tours.
 * Format:
 * - Header
 * - Metadata (sorted by key)
 * - "Found Solutions: N"
 * - For each solution, the 1-based step index followed by (row,col)
 */
public class TxtExporter implements ResultExporter {

    @Override
    public void exportSingle(List<Position> path, Map<String, Object> metadata, String filePath) {
        // Delegate to exportMultiple with a singleton list (null-safe).
        exportMultiple(path == null ? List.of() : List.of(path), metadata, filePath);
    }

    @Override
    public void exportMultiple(List<List<Position>> allPaths, Map<String, Object> metadata, String filePath) {
        // Null-safety
        List<List<Position>> tours = (allPaths == null) ? List.of() : allPaths;
        Map<String, Object> meta = (metadata == null) ? Map.of() : metadata;

        Path target = Paths.get(filePath);
        // Create parent directories if needed
        Path parent = target.getParent();
        try {
            if (parent != null)
                Files.createDirectories(parent);
        } catch (IOException e) {
            System.err.println("Failed to create directories for: " + target + " -> " + e.getMessage());
            return;
        }

        // Use UTF-8 and truncate existing files
        try (BufferedWriter writer = Files.newBufferedWriter(
                target, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            final String nl = System.lineSeparator();

            writer.write("Tour Export");
            writer.write(nl);
            writer.write("===============");
            writer.write(nl);

            // Deterministic metadata order
            if (!meta.isEmpty()) {
                Map<String, Object> sorted = new TreeMap<>(meta);
                for (Map.Entry<String, Object> e : sorted.entrySet()) {
                    writer.write(e.getKey() + ": " + String.valueOf(e.getValue()));
                    writer.write(nl);
                }
            }
            writer.write(nl);
            writer.write("Found Solutions: " + tours.size());
            writer.write(nl);
            writer.write(nl);

            int idx = 1;
            for (List<Position> tour : tours) {
                writer.write("Solution #" + (idx++) + ":");
                writer.write(nl);
                for (int i = 0; i < tour.size(); i++) {
                    Position p = tour.get(i);
                    // 1-based step number for readability
                    writer.write(String.format("%3d: (%d,%d)", i + 1, p.row(), p.col()));
                    writer.write(nl);
                }
                writer.write(nl);
            }

            // Library code shouldn't print to stdout normally; keeping minimal message for
            // continuity
            System.out.println("Tours successfully exported to " + target);

        } catch (IOException e) {
            System.err.println("Failed to export TXT to " + target + ": " + e.getMessage());
        }
    }
}
