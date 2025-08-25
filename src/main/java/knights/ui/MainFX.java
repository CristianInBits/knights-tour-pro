package knights.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import knights.export.JsonExporter;
import knights.export.ResultExporter;
import knights.export.TxtExporter;
import knights.model.Board;
import knights.model.Position;
import knights.solver.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class MainFX extends Application {
    private ControlsPane controls;
    private BoardView boardView;

    @Override
    public void start(Stage stage) {
        controls = new ControlsPane();
        boardView = new BoardView();

        BorderPane root = new BorderPane();
        root.setTop(controls);

        // Holder that keeps the board square and centered
        StackPane boardHolder = new StackPane(boardView);
        boardHolder.setPadding(new Insets(8)); // optional margin around the board
        boardHolder.setStyle("-fx-background-color: #222;"); // optional background for the board area

        // Bind the board’s size to the minimum of the holder’s width and height (force
        // 1:1 aspect ratio)
        var side = Bindings.min(boardHolder.widthProperty(), boardHolder.heightProperty());
        boardView.prefWidthProperty().bind(side);
        boardView.prefHeightProperty().bind(side);
        boardView.maxWidthProperty().bind(side);
        boardView.maxHeightProperty().bind(side);

        root.setCenter(boardHolder);

        // Hook Pause/Resume into the board animation
        controls.setOnPauseChanged(paused -> {
            if (paused)
                boardView.pauseAnimation();
            else
                boardView.resumeAnimation();
        });

        controls.setOnRun(cfg -> {
            controls.setRunning(true);
            Platform.runLater(() -> boardView.initGrid(cfg.rows(), cfg.cols()));

            CompletableFuture
                    .supplyAsync(() -> compute(cfg))
                    .whenComplete((result, error) -> Platform.runLater(() -> {
                        try {
                            if (error != null) {
                                controls.showMessage("Error: " + error.getMessage());
                                return;
                            }
                            if (result == null || result.path().isEmpty()) {
                                controls.showMessage("No solution found.");
                                return;
                            }

                            boardView.clearMarks();

                            // Enable Pause while animating; disable it when finished
                            controls.setAnimating(true);
                            boardView.setOnAnimationFinished(() -> controls.setAnimating(false));

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
                            } else {
                                controls.showMessage("Done");
                            }
                        } finally {
                            // Run button can be used again immediately; Pause stays enabled
                            // until animation finishes (handled by setOnAnimationFinished).
                            controls.setRunning(false);
                        }
                    }));
        });

        Scene scene = new Scene(root, 620, 900);
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
