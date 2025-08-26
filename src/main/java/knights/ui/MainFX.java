package knights.ui;

import knights.model.*;
import knights.solver.*;
import knights.export.*;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class MainFX extends Application {

    private ControlsPane controls;
    private BoardView boardView;

    // Dedicated executor to avoid contending with commonPool / solver parallel pool
    private final ExecutorService computeExec = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                var t = new Thread(r, "compute-exec");
                t.setDaemon(true);
                return t;
            });

    // Keep the running future to allow cancel (Stop)
    private final AtomicReference<CompletableFuture<SolveResult>> running = new AtomicReference<>();

    @Override
    public void start(Stage stage) {
        controls = new ControlsPane(); // now includes the right-side help panel
        boardView = new BoardView();

        BorderPane root = new BorderPane();

        // TOP: controls centered in an HBox wrapper
        HBox topBox = new HBox(controls);
        topBox.setAlignment(Pos.CENTER);
        controls.setMaxWidth(720); // give it room for grid + help
        controls.setPrefWidth(720);
        root.setTop(topBox);

        // CENTER: square, centered board holder
        StackPane boardHolder = new StackPane(boardView);
        boardHolder.setPadding(new Insets(8)); // optional margin around the board
        boardHolder.setStyle("-fx-background-color: #222;"); // board area background

        var side = Bindings.min(boardHolder.widthProperty(), boardHolder.heightProperty());
        boardView.prefWidthProperty().bind(side);
        boardView.prefHeightProperty().bind(side);
        boardView.maxWidthProperty().bind(side);
        boardView.maxHeightProperty().bind(side);
        root.setCenter(boardHolder);

        // Pause/Resume -> animation
        controls.setOnPauseChanged(paused -> {
            if (paused)
                boardView.pauseAnimation();
            else
                boardView.resumeAnimation();
        });

        // RUN: compute in background, then animate/export on FX thread
        controls.setOnRun(cfg -> {
            controls.setRunning(true);
            Platform.runLater(() -> boardView.initGrid(cfg.rows(), cfg.cols()));

            CompletableFuture<SolveResult> fut = CompletableFuture.supplyAsync(() -> compute(cfg), computeExec);

            running.set(fut);

            fut.whenComplete((result, error) -> Platform.runLater(() -> {
                // Ignore late/cancelled results
                var current = running.get();
                if (current == null || current.isCancelled() || current != fut)
                    return;

                try {
                    if (error != null) {
                        controls.showMessage("Error: " + error.getMessage());
                        System.out.println("[MainFX] compute error: " + error);
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
                        // Simple (sync) export; if files grow large, move to runAsync()
                        ResultExporter txt = new TxtExporter();
                        ResultExporter json = new JsonExporter();
                        try {
                            txt.exportSingle(result.path(), result.metadata(), cfg.exportDir() + "/tour.txt");
                            json.exportSingle(result.path(), result.metadata(), cfg.exportDir() + "/tour.json");
                            controls.showMessage("Exported to " + cfg.exportDir());
                        } catch (Exception ex) {
                            controls.showMessage("Export error: " + ex.getMessage());
                            System.out.println("[MainFX] export error: " + ex);
                        }
                    } else {
                        controls.showMessage("Done");
                    }
                } finally {
                    controls.setRunning(false);
                    running.set(null);
                }
            }));
        });

        // STOP: cancel compute and stop/clear animation
        controls.setOnStop(() -> {
            var fut = running.getAndSet(null);
            if (fut != null)
                fut.cancel(true); // cooperative cancel; we ignore any late result

            boardView.clearMarks(); // stop timeline + clear trail
            controls.setAnimating(false); // disable Pause, reset label
            controls.setRunning(false); // enable Run, disable Stop
            controls.showMessage("Stopped");
        });

        // Scene + CSS
        Scene scene = new Scene(root, 720, 900);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
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
            System.out.println("[MainFX] compute exception: " + e);
            return new SolveResult(List.of(), Map.of("error", e.getMessage()));
        }
    }

    private record SolveResult(List<Position> path, Map<String, Object> metadata) {
    }

    @Override
    public void stop() {
        computeExec.shutdownNow();
    }
}
