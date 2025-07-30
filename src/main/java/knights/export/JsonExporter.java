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
public class JsonExporter implements ResultExporter {

    @Override
    public void exportSingle(List<Position> path, Map<String, Object> metadata, String filePath) {
        exportMultiple(List.of(path), metadata, filePath);
    }

    @Override
    public void exportMultiple(List<List<Position>> paths, Map<String, Object> metadata, String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        var output = Map.of(
                "metadata", metadata,
                "paths", paths.stream()
                        .map(tour -> tour.stream()
                                .map(p -> Map.of("row", p.row(), "col", p.col()))
                                .toList())
                        .toList());

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(output, writer);
            System.out.println("Tours successfully exported to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to export JSON: " + e.getMessage());
        }
    }
}
