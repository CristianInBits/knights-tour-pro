package knights;

import knights.model.Board;
import knights.model.Position;

import knights.solver.*;
import knights.solver.parallel.ParallelBacktrackingSolver;

import knights.export.*;

import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println(
                    "Usage: java -jar knights-tour.jar <rows> <cols> <startRow> <startCol> <mode> <tourType> [strategy]");
            System.out.println("mode: single | all");
            System.out.println("tourType: open | closed");
            System.out.println("strategy (optional): backtrack (default) | warnsdorff | parallel");
            return;
        }

        int rows = Integer.parseInt(args[0]);
        int cols = Integer.parseInt(args[1]);
        int startRow = Integer.parseInt(args[2]);
        int startCol = Integer.parseInt(args[3]);
        String mode = args[4];
        String tourType = args[5];
        String strategy = args.length >= 7 ? args[6] : "backtrack";

        boolean isClosed = tourType.equalsIgnoreCase("closed");

        System.out.printf("Board: %dx%d | Start: (%d,%d) | Mode: %s | Tour: %s | Strategy: %s%n",
                rows, cols, startRow, startCol, mode,
                isClosed ? "closed" : "open",
                strategy);

        Board board = new Board(rows, cols);
        Position start = new Position(startRow, startCol);

        ResultExporter txtExporter = new TxtExporter();
        ResultExporter jsonExporter = new JsonExporter();

        Map<String, Object> metadata = Map.of(
                "rows", rows,
                "cols", cols,
                "startRow", startRow,
                "startCol", startCol,
                "tourType", tourType,
                "mode", mode,
                "strategy", strategy);

        if (mode.equalsIgnoreCase("all")) {
            List<List<Position>> allSolutions;

            if ("warnsdorff".equalsIgnoreCase(strategy)) {
                System.out.println("Warnsdorff strategy does not support generating all solutions.");
                return;
            } else if ("parallel".equalsIgnoreCase(strategy)) {
                ParallelBacktrackingSolver solver = new ParallelBacktrackingSolver(board, start, isClosed);
                allSolutions = solver.solveAll();
            } else {
                BacktrackingAllSolutionsSolver solver = new BacktrackingAllSolutionsSolver(board, start, isClosed);
                allSolutions = solver.solveAll();
            }

            System.out.printf("Found %d solutions.%n", allSolutions.size());

            int toPrint = Math.min(3, allSolutions.size());
            for (int i = 0; i < toPrint; i++) {
                System.out.printf("%nSolution #%d:%n", i + 1);
                board.reset();
                List<Position> solution = allSolutions.get(i);
                for (int j = 0; j < solution.size(); j++) {
                    board.mark(solution.get(j), j);
                }
                board.print();
            }

            // Export (multiple)
            txtExporter.exportMultiple(allSolutions, metadata, "output/tours.txt");
            jsonExporter.exportMultiple(allSolutions, metadata, "output/tours.json");

        } else { // single
            TourSolver solver;

            if ("warnsdorff".equalsIgnoreCase(strategy)) {
                solver = new WarnsdorffSolver(board, start, isClosed);
            } else if ("parallel".equalsIgnoreCase(strategy)) {
                // Parallel solver para una sola solución
                ParallelBacktrackingSolver psolver = new ParallelBacktrackingSolver(board, start, isClosed);
                List<Position> solution = psolver.solve();
                handleSingleResult(board, solution, txtExporter, jsonExporter, metadata);
                return;
            } else {
                solver = new BacktrackingSolver(board, start, isClosed);
            }

            List<Position> solution = solver.solve();
            handleSingleResult(board, solution, txtExporter, jsonExporter, metadata);
        }
    }

    private static void handleSingleResult(
            Board board,
            List<Position> solution,
            ResultExporter txtExporter,
            ResultExporter jsonExporter,
            Map<String, Object> metadata) {
        if (solution.isEmpty()) {
            System.out.println("No solution found.");
            return;
        }
        // Pintar matriz
        board.reset();
        for (int i = 0; i < solution.size(); i++) {
            board.mark(solution.get(i), i);
        }
        board.print();

        // Export (single)
        txtExporter.exportSingle(solution, metadata, "output/tour.txt");
        jsonExporter.exportSingle(solution, metadata, "output/tour.json");
    }
}
