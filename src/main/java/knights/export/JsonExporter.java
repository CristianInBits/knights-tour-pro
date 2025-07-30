package knights.export;

import knights.model.Position;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Exports a knight's tour solution to a JSON file.
 */
public class JsonExporter {

    /**
     * Exports a single solution to JSON format.
     *
     * @param path     the sequence of positions visited
     * @param metadata info like board size, start, tour type
     * @param filePath output file
     */
    public static void export(List<Position> path, Map<String, Object> metadata, String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        var output = Map.of(
                "metadata", metadata,
                "path", path.stream()
                        .map(p -> Map.of("row", p.row(), "col", p.col()))
                        .toList());

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(output, writer);
            System.out.println("Tour successfully exported to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to export JSON: " + e.getMessage());
        }
    }
}
