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
import java.util.Locale;
import java.util.Map;

/**
 * Draws the tours as an SVG: the board, the path the knight takes, and the step numbers.
 *
 * Unlike the TXT and JSON exports, this one is meant to be looked at rather than parsed.
 * The line joining the squares shades from indigo to cyan along the tour, so the order is
 * readable without following the numbers, and the starting square is outlined.
 *
 * Several tours are laid out in a grid. Note that 'all' mode can find hundreds of them, so
 * the file grows accordingly.
 */
public class SvgExporter implements ResultExporter {

    private static final int CELL = 56;
    private static final int PADDING = 20;
    private static final int CAPTION = 22;
    private static final int BOARDS_PER_ROW = 4;

    private static final String BACKGROUND = "#0e1014";
    private static final String LIGHT_SQUARE = "#242a36";
    private static final String DARK_SQUARE = "#1a1f28";
    private static final String TRAIL_START = "#6366f1";
    private static final String TRAIL_END = "#22d3ee";
    private static final String TEXT = "#8d95a6";
    private static final String START_OUTLINE = "#e8ebf2";

    @Override
    public void exportSingle(List<Position> path, Map<String, Object> metadata, String filePath) throws IOException {
        exportMultiple(path == null ? List.of() : List.of(path), metadata, filePath);
    }

    @Override
    public void exportMultiple(List<List<Position>> paths, Map<String, Object> metadata, String filePath)
            throws IOException {
        List<List<Position>> tours = (paths == null) ? List.of() : paths;
        Map<String, Object> meta = (metadata == null) ? Map.of() : metadata;

        int rows = dimension(meta, "rows", tours, true);
        int cols = dimension(meta, "cols", tours, false);

        int boardWidth = cols * CELL;
        int boardHeight = rows * CELL;
        int columns = Math.max(1, Math.min(BOARDS_PER_ROW, tours.size()));
        int gridRows = Math.max(1, (int) Math.ceil(tours.size() / (double) columns));

        int width = PADDING + columns * (boardWidth + PADDING);
        int height = PADDING + gridRows * (boardHeight + CAPTION + PADDING);

        Path target = Paths.get(filePath);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter out = Files.newBufferedWriter(
                target, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.write(String.format(Locale.ROOT,
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                    width, height, width, height));
            out.write("  <title>" + escape(title(meta, rows, cols, tours.size())) + "</title>\n");
            out.write("  <desc>" + escape(describe(meta)) + "</desc>\n");
            out.write(String.format(Locale.ROOT,
                    "  <rect width=\"%d\" height=\"%d\" fill=\"%s\"/>\n", width, height, BACKGROUND));

            for (int i = 0; i < tours.size(); i++) {
                int gx = PADDING + (i % columns) * (boardWidth + PADDING);
                int gy = PADDING + (i / columns) * (boardHeight + CAPTION + PADDING);
                writeBoard(out, tours.get(i), rows, cols, gx, gy, tours.size() > 1 ? (i + 1) : 0);
            }

            out.write("</svg>\n");
        }
    }

    // ===== Drawing =====

    private void writeBoard(BufferedWriter out, List<Position> tour, int rows, int cols,
            int originX, int originY, int number) throws IOException {

        out.write(String.format(Locale.ROOT, "  <g transform=\"translate(%d,%d)\">\n", originX, originY));

        if (number > 0) {
            out.write(String.format(Locale.ROOT,
                    "    <text x=\"0\" y=\"-6\" font-family=\"sans-serif\" font-size=\"13\" fill=\"%s\">"
                            + "Solution %d</text>\n",
                    TEXT, number));
        }

        // Squares
        out.write("    <g>\n");
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String fill = ((r + c) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
                out.write(String.format(Locale.ROOT,
                        "      <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"%s\"/>\n",
                        c * CELL, r * CELL, CELL, CELL, fill));
            }
        }
        out.write("    </g>\n");

        if (tour == null || tour.isEmpty()) {
            out.write("  </g>\n");
            return;
        }

        // The path, one segment per move so each can carry its own colour
        out.write("    <g stroke-width=\"3\" stroke-linecap=\"round\" fill=\"none\">\n");
        for (int i = 1; i < tour.size(); i++) {
            double t = (tour.size() > 2) ? (i - 1) / (double) (tour.size() - 2) : 0;
            out.write(String.format(Locale.ROOT,
                    "      <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"%s\"/>\n",
                    centreX(tour.get(i - 1)), centreY(tour.get(i - 1)),
                    centreX(tour.get(i)), centreY(tour.get(i)), blend(t)));
        }
        out.write("    </g>\n");

        // A dot on every square, and an outline on the one the knight started from
        out.write("    <g>\n");
        Position start = tour.get(0);
        out.write(String.format(Locale.ROOT,
                "      <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"none\" "
                        + "stroke=\"%s\" stroke-width=\"2\" stroke-opacity=\"0.6\"/>\n",
                start.col() * CELL + 1, start.row() * CELL + 1, CELL - 2, CELL - 2, START_OUTLINE));
        for (int i = 0; i < tour.size(); i++) {
            double t = (tour.size() > 1) ? i / (double) (tour.size() - 1) : 0;
            out.write(String.format(Locale.ROOT,
                    "      <circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"%s\"/>\n",
                    centreX(tour.get(i)), centreY(tour.get(i)), blend(t)));
        }
        out.write("    </g>\n");

        // Step numbers, tucked into the corner of each square
        out.write(String.format(Locale.ROOT,
                "    <g font-family=\"sans-serif\" font-size=\"11\" fill=\"%s\">\n", TEXT));
        for (int i = 0; i < tour.size(); i++) {
            Position p = tour.get(i);
            out.write(String.format(Locale.ROOT,
                    "      <text x=\"%d\" y=\"%d\">%d</text>\n",
                    p.col() * CELL + 5, p.row() * CELL + 14, i + 1));
        }
        out.write("    </g>\n");

        out.write("  </g>\n");
    }

    private static int centreX(Position p) {
        return p.col() * CELL + CELL / 2;
    }

    private static int centreY(Position p) {
        return p.row() * CELL + CELL / 2;
    }

    /** Mixes the two trail colours; t runs 0 at the first move to 1 at the last. */
    private static String blend(double t) {
        double clamped = Math.max(0, Math.min(1, t));
        int r = channel(TRAIL_START, 1, TRAIL_END, clamped);
        int g = channel(TRAIL_START, 3, TRAIL_END, clamped);
        int b = channel(TRAIL_START, 5, TRAIL_END, clamped);
        return String.format(Locale.ROOT, "#%02x%02x%02x", r, g, b);
    }

    private static int channel(String from, int offset, String to, double t) {
        int a = Integer.parseInt(from.substring(offset, offset + 2), 16);
        int b = Integer.parseInt(to.substring(offset, offset + 2), 16);
        return (int) Math.round(a + (b - a) * t);
    }

    // ===== Metadata =====

    /**
     * Board size from the metadata when it is there, otherwise worked out from the
     * squares the tour actually visits.
     */
    private static int dimension(Map<String, Object> meta, String key, List<List<Position>> tours, boolean isRow) {
        Object value = meta.get(key);
        if (value instanceof Number n && n.intValue() > 0) {
            return n.intValue();
        }
        int max = 0;
        for (List<Position> tour : tours) {
            if (tour == null) {
                continue;
            }
            for (Position p : tour) {
                max = Math.max(max, isRow ? p.row() : p.col());
            }
        }
        return max + 1;
    }

    private static String title(Map<String, Object> meta, int rows, int cols, int count) {
        String type = String.valueOf(meta.getOrDefault("tourType", "open"));
        return "Knight's tour " + rows + "x" + cols + " (" + type + "), " + count
                + (count == 1 ? " solution" : " solutions");
    }

    private static String describe(Map<String, Object> meta) {
        if (meta.isEmpty()) {
            return "No metadata";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : new java.util.TreeMap<>(meta).entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append(": ").append(e.getValue());
        }
        return sb.toString();
    }

    /** Metadata is free-form text, so it has to be safe to drop into XML. */
    private static String escape(String raw) {
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
