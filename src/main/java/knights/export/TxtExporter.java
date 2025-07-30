package knights.export;

import knights.model.Position;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Exports multiple knight's tour solutions to a TXT file.
 */
public class TxtExporter {

    /**
     * Exports multiple tours to a TXT file.
     *
     * @param allPaths list of tours (each is a list of positions)
     * @param metadata general info about the run
     * @param filePath output file path
     */
    public static void export(List<List<Position>> allPaths, Map<String, Object> metadata, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("Tour Export\n===============\n");
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
            }
            writer.write("\nFound Solutions: " + allPaths.size() + "\n\n");

            int count = 1;
            for (List<Position> tour : allPaths) {
                writer.write("Solution #" + count++ + ":\n");
                for (int i = 0; i < tour.size(); i++) {
                    Position p = tour.get(i);
                    writer.write(String.format("%3d: (%d,%d)\n", i + 1, p.row(), p.col()));
                }
                writer.write("\n");
            }

            System.out.println("Tours successfully exported to " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to export TXT: " + e.getMessage());
        }
    }
}
