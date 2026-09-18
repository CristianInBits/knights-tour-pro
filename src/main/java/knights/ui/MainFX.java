package knights.ui;

import knights.model.*;
import knights.solver.*;
import knights.export.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MainFX extends Application {

    private ControlsPane controls;
    private BoardView boardView;
    private Label placeholder;

    // Dedicated executor to avoid contending with commonPool / solver parallel pool
    private final ExecutorService computeExec = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                var t = new Thread(r, "compute-exec");
                t.setDaemon(true);
                return t;
            });

    /**
     * The search in progress, kept so Stop can cancel it. Submitted through the executor
     * rather than wrapped in a CompletableFuture: only a plain Future interrupts the
     * thread it runs on, and interrupting is what the solvers watch for.
     */
    private final AtomicReference<Future<?>> running = new AtomicReference<>();

    /**
     * Bumped by every Run and every Stop. A search carries the value it started with and
     * its result is dropped if that no longer matches, so a search that finishes just as
     * the user pressed Stop cannot redraw the board afterwards.
     */
    private final AtomicLong generation = new AtomicLong();

    @Override
    public void start(Stage stage) {
        controls = new ControlsPane(); // now includes the right-side help panel
        boardView = new BoardView();

        BorderPane root = new BorderPane();

        // LEFT: the controls, in a fixed-width column. The scroll pane only ever shows
        // itself on a short window; at the default size everything fits.
        ScrollPane sidebar = new ScrollPane(controls);
        sidebar.getStyleClass().add("sidebar-scroll");
        sidebar.setFitToWidth(true);
        sidebar.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebar.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebar.setMinViewportHeight(0);
        root.setLeft(sidebar);

        // CENTRE: the board gets the rest of the window, kept square and centred
        // Until the first run there is no board at all, so say so rather than leaving
        // the largest part of the window blank.
        placeholder = new Label("Set up the board and press Run");
        placeholder.getStyleClass().add("board-empty");

        StackPane boardHolder = new StackPane(placeholder, boardView);
        boardHolder.getStyleClass().add("board-area");
        boardHolder.setPadding(new Insets(28));

        var side = Bindings.min(
                boardHolder.widthProperty().subtract(56),
                boardHolder.heightProperty().subtract(56));
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
            final long startedAt = generation.incrementAndGet();
            controls.setRunning(true);
            placeholder.setVisible(false);
            boardView.initGrid(cfg.rows(), cfg.cols()); // already on the FX thread

            Future<?> task = computeExec.submit(() -> {
                SolveResult result = null;
                Throwable error = null;
                try {
                    result = compute(cfg);
                } catch (CancellationException stopped) {
                    return; // Stop already reset the UI; nothing left to report
                } catch (Throwable t) {
                    error = t;
                }
                final SolveResult computed = result;
                final Throwable failure = error;
                Platform.runLater(() -> showResult(cfg, startedAt, computed, failure));
            });

            running.set(task);
        });

        // STOP: interrupt the search and stop/clear the animation
        controls.setOnStop(() -> {
            generation.incrementAndGet(); // disown whatever is still in flight

            Future<?> task = running.getAndSet(null);
            if (task != null) {
                // Interrupts the compute thread; the solvers check for it and give up,
                // so the work really stops instead of running on unseen.
                task.cancel(true);
            }

            // Take the grid away, not just its marks: the placeholder sits behind the
            // board and would otherwise show through the gaps between the squares.
            boardView.clearBoard();
            placeholder.setVisible(true);
            controls.setAnimating(false); // disable Pause, reset label
            controls.setRunning(false); // enable Run
            controls.showMessage("Stopped");
        });

        // Scene + CSS
        Scene scene = new Scene(root, 1120, 760);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setTitle("Knight's Tour Pro — JavaFX");
        stage.setScene(scene);
        stage.setMinWidth(860);
        stage.setMinHeight(620);
        stage.show();
    }

    /** Runs on the FX thread once a search finishes. */
    private void showResult(ControlsPane.RunConfig cfg, long startedAt, SolveResult result, Throwable error) {
        if (generation.get() != startedAt) {
            return; // superseded by a later Run, or called off by Stop
        }
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
                // Simple (sync) export; if files grow large, move to a background task
                ResultExporter txt = new TxtExporter();
                ResultExporter json = new JsonExporter();
                ResultExporter svg = new SvgExporter();
                ResultExporter csv = new CsvExporter();
                try {
                    // A folder per run, so pressing Run twice keeps both results
                    Path runDir = RunDirectory.createUnder(Paths.get(cfg.exportDir()));
                    txt.exportSingle(result.path(), result.metadata(), runDir.resolve("tour.txt").toString());
                    json.exportSingle(result.path(), result.metadata(), runDir.resolve("tour.json").toString());
                    svg.exportSingle(result.path(), result.metadata(), runDir.resolve("tour.svg").toString());
                    csv.exportSingle(result.path(), result.metadata(), runDir.resolve("tour.csv").toString());
                    controls.showMessage("Exported to " + runDir.getFileName());
                } catch (IOException ex) {
                    // Reaching the user matters more than the tour itself being fine:
                    // silently skipping the files is how you lose a long run's results.
                    controls.showMessage("Export failed: " + ex.getMessage());
                    System.err.println("[MainFX] export error: " + ex);
                }
            } else {
                controls.showMessage("Done");
            }
        } finally {
            controls.setRunning(false);
            running.set(null);
        }
    }

    private SolveResult compute(ControlsPane.RunConfig cfg) {
        try {
            Board board = new Board(cfg.rows(), cfg.cols());
            Position start = new Position(cfg.startRow(), cfg.startCol());
            boolean closed = cfg.closed();

            if (cfg.mode().equalsIgnoreCase("all")) {
                AllToursSolver allSolver = switch (cfg.strategy().toLowerCase()) {
                    case "backtrack" -> new BacktrackingAllSolutionsSolver(board, start, closed);
                    case "parallel" -> new ParallelAllToursSolver(board, start, closed, cfg.forkDepth());
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
                                // shutdownNow, not shutdown: on cancellation the workers have
                                // already been told to unwind and nothing should outlive this.
                                pool.shutdownNow();
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
        } catch (CancellationException stopped) {
            // Stop was pressed. Let it through: turning it into a result here would make
            // a cancelled search look like a board with no solution.
            throw stopped;
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
