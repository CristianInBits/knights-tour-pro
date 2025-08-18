package knights.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.Map;

import knights.export.JsonExporter;
import knights.export.ResultExporter;
import knights.export.TxtExporter;
import knights.model.Board;
import knights.model.Position;
import knights.solver.*;

public class MainFX extends Application {

    private ControlsPane controls;
    private BoardView boardView;

    @Override
    public void start(Stage stage) {
        controls = new ControlsPane();
        boardView = new BoardView();

        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(boardView);

        controls.setOnRun(cfg -> {
            Platform.runLater(() -> boardView.initGrid(cfg.rows(), cfg.cols()));

            CompletableFuture
                    .supplyAsync(() -> compute(cfg))
                    .thenAccept(result -> Platform.runLater(() -> {
                        if (result == null || result.path().isEmpty()) {
                            controls.showMessage("No solution found.");
                            return;
                        }
                        // Animar
                        boardView.clearMarks();
                        boardView.animate(result.path(), cfg.msPerStep());

                        if (cfg.export()) {
                            ResultExporter txt = new TxtExporter();
                            ResultExporter json = new JsonExporter();
                            try {
                                txt.exportSingle(result.path(), result.metadata(), cfg.exportDir() + "/tour.txt");
                                json.exportSingle(result.path(), result.metadata(), cfg.exportDir() + "/tour.json");
                                controls.showMessage("Exported to " + cfg.exportDir());
                            } catch (Exception ex) {
                                controls.showMessage("Export error: " + ex.getMessage());
                            }
                        }
                    }));
        });

        Scene scene = new Scene(root, 900, 720);
        stage.setTitle("Knight's Tour Pro — JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    private SolveResult compute(ControlsPane.RunConfig cfg) {
        try {
            Board board = new Board(cfg.rows(), cfg.cols());
            Position start = new Position(cfg.startRow(), cfg.startCol());
            boolean closed = cfg.closed();

            if (cfg.mode().equalsIgnoreCase("all")) {
                AllToursSolver allSolver = switch (cfg.strategy().toLowerCase()) {
                    case "backtrack" -> new BacktrackingAllSolutionsSolver(board, start, closed);
                    default -> null;
                };
                if (allSolver == null)
                    return new SolveResult(List.of(), cfg.metadata("all"));
                List<List<Position>> all = allSolver.solveAll();
                List<Position> first = all.isEmpty() ? List.of() : all.get(0);
                return new SolveResult(first, cfg.metadata("all"));
            } else {
                // Single
                TourSolver solver;
                switch (cfg.strategy().toLowerCase()) {
                    case "warnsdorff" -> solver = new WarnsdorffSolver(board, start, closed);
                    case "parallel" -> {
                        if (cfg.poolParallelism() != null) {
                            ForkJoinPool pool = new ForkJoinPool(cfg.poolParallelism());
                            try {
                                List<Position> path = new ParallelBacktrackingSolver(
                                        board, start, closed, cfg.forkDepth(), pool).solve();
                                return new SolveResult(path, cfg.metadata("single"));
                            } finally {
                                pool.shutdown();
                            }
                        } else {
                            solver = new ParallelBacktrackingSolver(board, start, closed, cfg.forkDepth());
                        }
                    }

                    default -> solver = new BacktrackingSolver(board, start, closed);
                }
                List<Position> path = solver.solve();
                return new SolveResult(path, cfg.metadata("single"));
            }
        } catch (Exception e) {
            return new SolveResult(List.of(), Map.of("error", e.getMessage()));
        }
    }

    private record SolveResult(List<Position> path, Map<String, Object> metadata) {
    }
}
