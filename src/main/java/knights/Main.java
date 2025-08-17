package knights;

import knights.export.JsonExporter;
import knights.export.ResultExporter;
import knights.export.TxtExporter;
import knights.model.Board;
import knights.model.Position;
import knights.solver.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CLI entry point for Knight's Tour.
 *
 * Usage (positional, backward compatible):
 * java -jar knights-tour.jar <rows> <cols> <startRow> <startCol> <mode>
 * <tourType> [strategy]
 * - mode: single | all
 * - tourType: open | closed
 * - strategy (optional): backtrack (default) | warnsdorff | parallel
 *
 * Optional flags (order-agnostic, after the positional args):
 * --limit N : limit how many solutions are printed to console in 'all' mode
 * (default: 3)
 * --out DIR : base output directory (default: output)
 * --no-print : do not print board(s) to console
 * --no-export : do not write TXT/JSON files
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length < 6) {
            printUsage();
            return;
        }

        // ---- Parse positional arguments ----
        final int rows, cols, startRow, startCol;
        final String modeRaw, tourTypeRaw, strategyRaw;
        try {
            rows = Integer.parseInt(args[0]);
            cols = Integer.parseInt(args[1]);
            startRow = Integer.parseInt(args[2]);
            startCol = Integer.parseInt(args[3]);
        } catch (NumberFormatException nfe) {
            System.err.println("Error: rows, cols, startRow, startCol must be integers.");
            printUsage();
            return;
        }

        modeRaw = args[4];
        tourTypeRaw = args[5];
        strategyRaw = (args.length >= 7) ? args[6] : "backtrack";

        final boolean isClosed = "closed".equalsIgnoreCase(tourTypeRaw);
        final String mode = modeRaw.toLowerCase(Locale.ROOT);
        final String strategy = strategyRaw.toLowerCase(Locale.ROOT);

        // ---- Parse optional flags ----
        int limitToPrint = 3; // only affects 'all' mode, console printing
        String outDir = "output"; // base output folder
        boolean printBoards = true; // console printing
        boolean doExport = true; // write files

        if (args.length > 7) {
            for (int i = 7; i < args.length; i++) {
                String a = args[i];
                if (a == null)
                    continue;
                a = a.trim();
                if (a.isEmpty())
                    continue;

                if (a.startsWith("--limit")) {
                    // --limit or --limit=5 or "--limit 5"
                    String val = null;
                    if (a.contains("=")) {
                        val = a.substring(a.indexOf('=') + 1);
                    } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        val = args[++i];
                    }
                    if (val == null) {
                        System.err.println("Missing value for --limit");
                        return;
                    }
                    try {
                        limitToPrint = Math.max(0, Integer.parseInt(val));
                    } catch (NumberFormatException nfe) {
                        System.err.println("Invalid --limit value: " + val);
                        return;
                    }
                } else if (a.startsWith("--out")) {
                    String val = null;
                    if (a.contains("=")) {
                        val = a.substring(a.indexOf('=') + 1);
                    } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        val = args[++i];
                    }
                    if (val == null || val.isBlank()) {
                        System.err.println("Missing value for --out");
                        return;
                    }
                    outDir = val;
                } else if ("--no-print".equalsIgnoreCase(a)) {
                    printBoards = false;
                } else if ("--no-export".equalsIgnoreCase(a)) {
                    doExport = false;
                } else {
                    System.err.println("Unknown flag: " + a);
                    printUsage();
                    return;
                }
            }
        }

        // ---- Basic validations ----
        if (rows <= 0 || cols <= 0) {
            System.err.println("Error: rows and cols must be > 0.");
            return;
        }
        if (startRow < 0 || startRow >= rows || startCol < 0 || startCol >= cols) {
            System.err.printf("Error: start position (%d,%d) is outside the board %dx%d.%n",
                    startRow, startCol, rows, cols);
            return;
        }
        if (!mode.equals("single") && !mode.equals("all")) {
            System.err.println("Error: mode must be 'single' or 'all'.");
            return;
        }
        if (!strategy.equals("backtrack") && !strategy.equals("warnsdorff") && !strategy.equals("parallel")) {
            System.err.println("Error: strategy must be 'backtrack', 'warnsdorff', or 'parallel'.");
            return;
        }

        // ---- Setup board, start, metadata ----
        Board board = new Board(rows, cols);
        Position start = new Position(startRow, startCol);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("rows", rows);
        metadata.put("cols", cols);
        metadata.put("startRow", startRow);
        metadata.put("startCol", startCol);
        metadata.put("tourType", isClosed ? "closed" : "open");
        metadata.put("mode", mode);
        metadata.put("strategy", strategy);
        metadata.put("timestamp", ts);

        System.out.printf("Board: %dx%d | Start: (%d,%d) | Mode: %s | Tour: %s | Strategy: %s%n",
                rows, cols, startRow, startCol, mode, (isClosed ? "closed" : "open"), strategy);

        // ---- Choose exporters ----
        ResultExporter txtExporter = new TxtExporter();
        ResultExporter jsonExporter = new JsonExporter();

        // Ensure output directory exists (only if exporting)
        Path outBase = Paths.get(outDir);
        if (doExport) {
            try {
                Files.createDirectories(outBase);
            } catch (Exception e) {
                System.err.println("Failed to create output directory '" + outBase + "': " + e.getMessage());
                return;
            }
        }

        // ---- Run according to mode ----
        long t0 = System.nanoTime();
        if (mode.equals("all")) {
            // 'all' requires a solver that can enumerate all solutions
            AllToursSolver allSolver = buildAllSolver(strategy, board, start, isClosed);
            if (allSolver == null) {
                System.out.println("Selected strategy does not support 'all' mode. Try 'backtrack'.");
                return;
            }

            List<List<Position>> allSolutions = allSolver.solveAll();
            long t1 = System.nanoTime();
            System.out.printf("Found %d solution(s) in %.3f ms.%n",
                    allSolutions.size(), (t1 - t0) / 1e6);

            // Print first N solutions (re-markboard for pretty print)
            if (printBoards && limitToPrint > 0) {
                int toPrint = Math.min(limitToPrint, allSolutions.size());
                for (int i = 0; i < toPrint; i++) {
                    System.out.printf("%nSolution #%d:%n", i + 1);
                    board.reset();
                    List<Position> sol = allSolutions.get(i);
                    for (int j = 0; j < sol.size(); j++) {
                        board.mark(sol.get(j), j);
                    }
                    board.print();
                }
            }

            if (doExport) {
                // output/tours.txt and output/tours.json (keep stable naming)
                Path txt = outBase.resolve("tours.txt");
                Path json = outBase.resolve("tours.json");
                txtExporter.exportMultiple(allSolutions, metadata, txt.toString());
                jsonExporter.exportMultiple(allSolutions, metadata, json.toString());
            }

        } else { // single
            List<Position> solution;
            TourSolver solver = buildSingleSolver(strategy, board, start, isClosed);
            solution = solver.solve();
            long t1 = System.nanoTime();

            if (solution.isEmpty()) {
                System.out.printf("No solution found (%.3f ms).%n", (t1 - t0) / 1e6);
                return;
            }

            System.out.printf("Solution found in %.3f ms.%n", (t1 - t0) / 1e6);

            if (printBoards) {
                // Re-mark for pretty print
                board.reset();
                for (int i = 0; i < solution.size(); i++) {
                    board.mark(solution.get(i), i);
                }
                board.print();
            }

            if (doExport) {
                // output/tour.txt and output/tour.json
                Path txt = outBase.resolve("tour.txt");
                Path json = outBase.resolve("tour.json");
                txtExporter.exportSingle(solution, metadata, txt.toString());
                jsonExporter.exportSingle(solution, metadata, json.toString());
            }
        }
    }

    // ---- Helpers ----

    private static void printUsage() {
        System.out.println(
                "Usage: java -jar knights-tour.jar <rows> <cols> <startRow> <startCol> <mode> <tourType> [strategy]");
        System.out.println("  mode     : single | all");
        System.out.println("  tourType : open | closed");
        System.out.println("  strategy : backtrack (default) | warnsdorff | parallel");
        System.out.println();
        System.out.println("Optional flags:");
        System.out.println("  --limit N    : limit how many solutions are printed in 'all' mode (default: 3)");
        System.out.println("  --out DIR    : base output directory (default: output)");
        System.out.println("  --no-print   : do not print board(s) to console");
        System.out.println("  --no-export  : do not write TXT/JSON files");
    }

    /**
     * Build a solver for 'single' mode.
     */
    private static TourSolver buildSingleSolver(String strategy, Board board, Position start, boolean isClosed) {
        switch (strategy) {
            case "warnsdorff":
                return new WarnsdorffSolver(board, start, isClosed);
            case "parallel":
                // If you have a configurable forkDepth, read from env/flag; using 2 as a
                // sensible default.
                int forkDepth = 2;
                return new ParallelBacktrackingSolver(board, start, isClosed, forkDepth);
            case "backtrack":
            default:
                return new BacktrackingSolver(board, start, isClosed);
        }
    }

    /**
     * Build a solver for 'all' mode. Returns null if strategy doesn't support
     * enumeration.
     */
    private static AllToursSolver buildAllSolver(String strategy, Board board, Position start, boolean isClosed) {
        switch (strategy) {
            case "backtrack":
                return new BacktrackingAllSolutionsSolver(board, start, isClosed);
            case "parallel":
                // parallel all-solutions solver, return it here.
                System.out.println("Parallel strategy does not support 'all' mode yet. Falling back is disabled.");
                return null;
            case "warnsdorff":
            default:
                System.out.println("Warnsdorff strategy does not support 'all' mode.");
                return null;
        }
    }
}
