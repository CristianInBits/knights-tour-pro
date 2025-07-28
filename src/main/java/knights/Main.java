package knights;

import knights.model.Board;
import knights.model.Position;
import knights.solver.BacktrackingAllSolutionsSolver;
import knights.solver.BacktrackingSolver;
import knights.solver.TourSolver;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: java -jar knights-tour.jar <rows> <cols> <startRow> <startCol> <mode>");
            System.out.println("Example: java -jar knights-tour.jar 5 5 0 0 single");
            return;
        }

        int rows = Integer.parseInt(args[0]);
        int cols = Integer.parseInt(args[1]);
        int startRow = Integer.parseInt(args[2]);
        int startCol = Integer.parseInt(args[3]);
        String mode = args[4];

        System.out.printf("Board: %dx%d | Start: (%d,%d) | Mode: %s\n",
                rows, cols, startRow, startCol, mode);

        Board board = new Board(rows, cols);
        Position start = new Position(startRow, startCol);

        if (mode.equalsIgnoreCase("all")) {
            BacktrackingAllSolutionsSolver solver = new BacktrackingAllSolutionsSolver(board, start, false);
            List<List<Position>> allSolutions = solver.solveAll();
            System.out.printf("Found %d solutions.%n", allSolutions.size());

            int toPrint = Math.min(3, allSolutions.size());
            for (int i = 0; i < toPrint; i++) {
                System.out.printf("\nSolution #%d:%n", i + 1);
                board.reset();
                List<Position> solution = allSolutions.get(i);
                for (int j = 0; j < solution.size(); j++) {
                    board.mark(solution.get(j), j);
                }
                board.print();
            }
        } else {
            TourSolver solver = new BacktrackingSolver(board, start, false);
            List<Position> solution = solver.solve();
            if (solution.isEmpty()) {
                System.out.println("No solution found.");
            } else {
                for (int i = 0; i < solution.size(); i++) {
                    board.mark(solution.get(i), i);
                }
                board.print();
            }
        }
    }
}
