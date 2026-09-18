package knights;

import knights.export.JsonExporter;
import knights.export.ResultExporter;
import knights.export.TxtExporter;
import knights.model.Board;
import knights.model.Position;
import knights.solver.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

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
 * --limit N : limit how many solutions are printed in 'all' mode (default: 3)
 * --out DIR : base output directory (default: output)
 * --no-print : do not print board(s) to console
 * --no-export : do not write TXT/JSON files
 * --fork-depth N : parallel backtracking fork depth (default: 2)
 * --pool N : create a custom ForkJoinPool with parallelism N (default: common
 * pool)
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

        // 'strategy' is optional. When the 7th argument already looks like a flag, keep the
        // default strategy and start reading flags there instead of swallowing one.
        final int firstFlag;
        if (args.length >= 7 && !args[6].startsWith("--")) {
            strategyRaw = args[6];
            firstFlag = 7;
        } else {
            strategyRaw = "backtrack";
            firstFlag = 6;
        }

        final boolean isClosed = "closed".equalsIgnoreCase(tourTypeRaw);
        final String mode = modeRaw.toLowerCase(Locale.ROOT);
        final String strategy = strategyRaw.toLowerCase(Locale.ROOT);

        // ---- Parse optional flags ----
        int limitToPrint = 3; // only affects 'all' mode, console printing
        String outDir = "output"; // base output folder
        boolean printBoards = true; // console printing
        boolean doExport = true; // write files

        // Parallel-specific tuning (defaults)
        int forkDepth = 2; // sensible default for parallel
        Integer poolParallelism = null; // null => use common pool

        for (int i = firstFlag; i < args.length; i++) {
            String token = (args[i] == null) ? "" : args[i].trim();
            if (token.isEmpty())
                continue;

            // Accept both "--name=value" and "--name value". The name is compared in full,
            // so a typo such as "--outrageous" is rejected instead of passing for "--out".
            String name = token;
            String value = null;
            int eq = token.indexOf('=');
            if (eq >= 0) {
                name = token.substring(0, eq);
                value = token.substring(eq + 1);
            }
            name = name.toLowerCase(Locale.ROOT);

            final boolean takesValue;
            if (name.equals("--no-print") || name.equals("--no-export")) {
                takesValue = false;
            } else if (name.equals("--limit") || name.equals("--out")
                    || name.equals("--fork-depth") || name.equals("--pool")) {
                takesValue = true;
            } else {
                System.err.println("Unknown flag: " + token);
                printUsage();
                return;
            }

            if (takesValue && value == null) {
                if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                    System.err.println("Missing value for " + name);
                    return;
                }
                value = args[++i];
            } else if (!takesValue && value != null) {
                System.err.println(name + " does not take a value.");
                return;
            }

            try {
                switch (name) {
                    case "--no-print" -> printBoards = false;
                    case "--no-export" -> doExport = false;
                    case "--limit" -> limitToPrint = Math.max(0, Integer.parseInt(value));
                    case "--fork-depth" -> forkDepth = Math.max(0, Integer.parseInt(value));
                    case "--out" -> {
                        if (value.isBlank()) {
                            System.err.println("Missing value for --out");
                            return;
                        }
                        outDir = value;
                    }
                    case "--pool" -> {
                        int p = Integer.parseInt(value);
                        if (p <= 0) {
                            System.err.println("--pool must be > 0");
                            return;
                        }
                        poolParallelism = p;
                    }
                }
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid " + name + " value: " + value);
                return;
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
        // Add parallel-specific metadata when relevant
        if ("parallel".equals(strategy)) {
            metadata.put("forkDepth", forkDepth);
            metadata.put("pool", (poolParallelism == null) ? "common" : poolParallelism);
        }
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

            // Print first N solutions (re-mark board for pretty print)
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
                Path txt = outBase.resolve("tours.txt");
                Path json = outBase.resolve("tours.json");
                try {
                    txtExporter.exportMultiple(allSolutions, metadata, txt.toString());
                    jsonExporter.exportMultiple(allSolutions, metadata, json.toString());
                    System.out.println("Exported to " + txt + " and " + json);
                } catch (IOException e) {
                    System.err.println("Failed to export: " + e.getMessage());
                    return;
                }
            }

        } else { // single
            List<Position> solution;
            // Special-case 'parallel' to pass forkDepth & optional custom pool
            if ("parallel".equals(strategy)) {
                ForkJoinPool customPool = null;
                try {
                    TourSolver solver;
                    if (poolParallelism != null) {
                        // Create a dedicated pool with the requested parallelism
                        customPool = new ForkJoinPool(poolParallelism);
                        solver = new ParallelBacktrackingSolver(board, start, isClosed, forkDepth, customPool);
                    } else {
                        // Use common pool
                        solver = new ParallelBacktrackingSolver(board, start, isClosed, forkDepth);
                    }
                    solution = solver.solve();
                } finally {
                    // If we created a custom pool, we should shut it down
                    if (customPool != null)
                        customPool.shutdown();
                }
            } else {
                // Backtrack / Warnsdorff unchanged
                TourSolver solver = buildSingleSolver(strategy, board, start, isClosed);
                solution = solver.solve();
            }

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
                Path txt = outBase.resolve("tour.txt");
                Path json = outBase.resolve("tour.json");
                try {
                    txtExporter.exportSingle(solution, metadata, txt.toString());
                    jsonExporter.exportSingle(solution, metadata, json.toString());
                    System.out.println("Exported to " + txt + " and " + json);
                } catch (IOException e) {
                    System.err.println("Failed to export: " + e.getMessage());
                    return;
                }
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
        System.out.println("  --limit N       : limit how many solutions are printed in 'all' mode (default: 3)");
        System.out.println("  --out DIR       : base output directory (default: output)");
        System.out.println("  --no-print      : do not print board(s) to console");
        System.out.println("  --no-export     : do not write TXT/JSON files");
        System.out.println("  --fork-depth N  : parallel backtracking fork depth (default: 2)");
        System.out
                .println("  --pool N        : create a custom ForkJoinPool with parallelism N (default: common pool)");
    }

    /**
     * Build a solver for 'single' mode (non-parallel strategies).
     */
    private static TourSolver buildSingleSolver(String strategy, Board board, Position start, boolean isClosed) {
        switch (strategy) {
            case "warnsdorff":
                return new WarnsdorffSolver(board, start, isClosed);
            case "parallel":
                // Not used in this overload (handled in caller to pass forkDepth/pool)
                // Kept for backward compatibility.
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
                System.out.println("Parallel strategy does not support 'all' mode yet. Falling back is disabled.");
                return null;
            case "warnsdorff":
            default:
                System.out.println("Warnsdorff strategy does not support 'all' mode.");
                return null;
        }
    }
}
