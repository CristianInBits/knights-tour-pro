package knights.export;

import knights.model.Position;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * Writes the tours as CSV, one row per move:
 *
 * <pre>
 * solution,step,row,col
 * 1,1,0,0
 * 1,2,1,2
 * </pre>
 *
 * One row per move rather than one per tour, because that is the shape spreadsheets and
 * dataframes expect: sorting, filtering and pivoting all work without reshaping anything
 * first. In 'single' mode the solution column is always 1, which keeps the columns the
 * same whichever mode produced the file, so several runs can be concatenated.
 *
 * Steps are numbered from 1, matching the text export. Metadata is deliberately left out:
 * it is the same on every row, and it would stop the file loading cleanly. The TXT and
 * JSON exports carry it.
 *
 * Follows RFC 4180: comma separated, CRLF line endings. Every value is an integer, so no
 * quoting or escaping is ever needed.
 */
public class CsvExporter implements ResultExporter {

    private static final String HEADER = "solution,step,row,col";
    private static final String CRLF = "\r\n";

    @Override
    public void exportSingle(List<Position> path, Map<String, Object> metadata, String filePath) throws IOException {
        exportMultiple(path == null ? List.of() : List.of(path), metadata, filePath);
    }

    @Override
    public void exportMultiple(List<List<Position>> paths, Map<String, Object> metadata, String filePath)
            throws IOException {
        List<List<Position>> tours = (paths == null) ? List.of() : paths;

        Path target = Paths.get(filePath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter out = Files.newBufferedWriter(
                target, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            out.write(HEADER);
            out.write(CRLF);

            for (int solution = 0; solution < tours.size(); solution++) {
                List<Position> tour = tours.get(solution);
                if (tour == null) {
                    continue;
                }
                for (int step = 0; step < tour.size(); step++) {
                    Position p = tour.get(step);
                    out.write(Integer.toString(solution + 1));
                    out.write(',');
                    out.write(Integer.toString(step + 1));
                    out.write(',');
                    out.write(Integer.toString(p.row()));
                    out.write(',');
                    out.write(Integer.toString(p.col()));
                    out.write(CRLF);
                }
            }
        }
    }
}
