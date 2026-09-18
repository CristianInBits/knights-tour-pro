package knights.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import knights.model.Position;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

/**
 * JSON exporter for Knight's Tours (UTF-8, pretty printed).
 * Structure:
 * {
 * "metadata": { ... },
 * "paths": [
 * [ {"row":r,"col":c}, ... ],
 * ...
 * ]
 * }
 */
public class JsonExporter implements ResultExporter {

    @Override
    public void exportSingle(List<Position> path, Map<String, Object> metadata, String filePath) {
        exportMultiple(path == null ? List.of() : List.of(path), metadata, filePath);
    }

    @Override
    public void exportMultiple(List<List<Position>> paths, Map<String, Object> metadata, String filePath) {
        List<List<Position>> tours = (paths == null) ? List.of() : paths;
        Map<String, Object> meta = (metadata == null) ? Map.of() : metadata;

        Path target = Paths.get(filePath);
        Path parent = target.getParent();
        try {
            if (parent != null)
                Files.createDirectories(parent);
        } catch (IOException e) {
            System.err.println("Failed to create directories for: " + target + " -> " + e.getMessage());
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (BufferedWriter bw = Files.newBufferedWriter(
                target, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                JsonWriter jw = new JsonWriter(bw)) {

            jw.setIndent("  "); // pretty print

            jw.beginObject();

            // metadata (let Gson handle arbitrary value types)
            jw.name("metadata");
            gson.toJson(meta, Map.class, jw);

            // paths
            jw.name("paths");
            jw.beginArray();
            for (List<Position> tour : tours) {
                jw.beginArray();
                for (Position p : tour) {
                    jw.beginObject();
                    jw.name("row").value(p.row());
                    jw.name("col").value(p.col());
                    jw.endObject();
                }
                jw.endArray();
            }
            jw.endArray();

            jw.endObject();

            System.out.println("Tours successfully exported to " + target);

        } catch (IOException e) {
            System.err.println("Failed to export JSON to " + target + ": " + e.getMessage());
        }
    }
}
