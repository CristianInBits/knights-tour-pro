package knights.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ControlsPane extends VBox {

    private final Spinner<Integer> spRows = new Spinner<>(1, 20, 6);
    private final Spinner<Integer> spCols = new Spinner<>(1, 20, 6);
    private final Spinner<Integer> spSR = new Spinner<>(0, 19, 0);
    private final Spinner<Integer> spSC = new Spinner<>(0, 19, 0);
    private final Spinner<Integer> spPool = new Spinner<>(1, 64,
            Math.max(2, Runtime.getRuntime().availableProcessors()));
    private final Spinner<Integer> spFork = new Spinner<>(0, 8, 2);

    private final ChoiceBox<String> cbMode = new ChoiceBox<>();
    private final ChoiceBox<String> cbStrategy = new ChoiceBox<>();

    private final CheckBox chkClosed = new CheckBox("Closed");
    private final CheckBox chkUseCustomPool = new CheckBox("Custom Pool");

    private final Slider slSpeed = new Slider(10, 400, 80); // ms per step
    private final Text lblSpeed = new Text("Speed: 80 ms/step");
    private final TextField tfOut = new TextField("output");
    private final CheckBox chkExport = new CheckBox("Export");
    private final Label status = new Label();

    private final Button btnRun = new Button("Run");
    private final Button btnPause = new Button("Pause");
    private final Button btnStop = new Button("Stop");

    // Help panel (right side inside ControlsPane)
    private final Label helpTitle = new Label("Description");
    private final TextFlow helpText = new TextFlow();
    private final VBox helpPane = new VBox(8, helpTitle, helpText);

    private Consumer<RunConfig> onRun;
    private Consumer<Boolean> onPauseChanged;
    private Runnable onStop;

    private boolean paused = false;

    public ControlsPane() {
        // Panel container config
        setPadding(new Insets(8, 16, 8, 16));
        setSpacing(6);
        getStyleClass().add("controls");

        // Data & defaults
        cbMode.getItems().addAll("single", "all");
        cbMode.getSelectionModel().select("single");

        cbStrategy.getItems().addAll("backtrack", "warnsdorff", "parallel");
        cbStrategy.getSelectionModel().select("backtrack");

        chkExport.setSelected(false);
        chkUseCustomPool.setSelected(false);
        spFork.setDisable(true);
        spPool.setDisable(true);

        // Reactive enable/disable for parallel options
        cbStrategy.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean isParallel = "parallel".equalsIgnoreCase(newV);
            spFork.setDisable(!isParallel);
            chkUseCustomPool.setDisable(!isParallel);
            spPool.setDisable(!isParallel || !chkUseCustomPool.isSelected());
            refreshHelp(); // update help text when strategy changes
        });
        chkUseCustomPool.selectedProperty().addListener((obs, oldV, newV) -> {
            spPool.setDisable(!newV || !"parallel".equalsIgnoreCase(cbStrategy.getValue()));
        });

        // Keep start spinners within rows/cols range
        spRows.valueProperty().addListener((o, ov, nv) -> spSR.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, nv - 1, Math.min(spSR.getValue(), nv - 1))));
        spCols.valueProperty().addListener((o, ov, nv) -> spSC.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, nv - 1, Math.min(spSC.getValue(), nv - 1))));

        // Speed label binding
        slSpeed.valueProperty().addListener((obs, ov, nv) -> lblSpeed.setText("Speed: " + nv.intValue() + " ms/step"));

        // Spinners styling & UX
        Stream.of(spRows, spCols, spSR, spSC, spFork, spPool).forEach(sp -> {
            sp.getStyleClass().addAll("ghost"); // you kept Ghost
            sp.setEditable(true);
            sp.setPrefWidth(110);
            enableScroll(sp);
            selectAllOnFocus(sp);
            centerText(sp);
        });

        // Remove duplicated labels on checkboxes (we keep Label at the left column)
        chkClosed.setText(null);
        chkUseCustomPool.setText(null);
        chkExport.setText(null);

        // Build left grid with controls
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        int r = 0;
        grid.add(row(new Label("Rows"), spRows, new Label("Cols"), spCols), 0, r++);
        grid.add(row(new Label("Start Row"), spSR, new Label("Start Col"), spSC), 0, r++);
        grid.add(row(new Label("Mode"), cbMode, new Label("Strategy"), cbStrategy), 0, r++);
        grid.add(row(new Label("Closed"), chkClosed, new Label("Fork Depth"), spFork), 0, r++);
        grid.add(row(new Label("Custom Pool"), chkUseCustomPool, new Label("Pool size"), spPool), 0, r++);
        grid.add(row(lblSpeed, slSpeed), 0, r++); // keep Text as Node for custom color
        grid.add(row(new Label("Output dir"), tfOut, new Label("Export"), chkExport), 0, r++);

        // Right help panel (inside ControlsPane)
        helpTitle.getStyleClass().add("help-title");
        helpText.getStyleClass().add("help-text");
        helpText.setPrefWidth(280);
        helpText.setMaxWidth(280);
        helpText.setLineSpacing(4);
        helpPane.setPadding(new Insets(8, 0, 0, 16)); // small gap from grid
        helpPane.getStyleClass().add("help-pane");

        // Main row: grid + help side by side
        HBox mainRow = new HBox(16, grid, helpPane);
        HBox.setHgrow(grid, Priority.ALWAYS);
        // helpPane keeps a fixed narrow column width (280px)
        getChildren().add(mainRow);

        // Actions row
        HBox actions = new HBox(10, btnRun, btnPause, btnStop, status);
        actions.setPadding(new Insets(4, 0, 0, 0));
        actions.getStyleClass().add("actions");
        getChildren().add(actions);

        // Handlers
        btnRun.setOnAction(e -> {
            RunConfig cfg = currentConfig();
            if (onRun != null) {
                status.setText("Running…");
                onRun.accept(cfg);
            }
        });

        btnPause.setOnAction(e -> {
            paused = !paused;
            btnPause.setText(paused ? "Resume" : "Pause");
            if (onPauseChanged != null)
                onPauseChanged.accept(paused);
        });

        btnStop.setOnAction(e -> {
            if (onStop != null)
                onStop.run();
        });

        // CSS classes
        lblSpeed.getStyleClass().add("speed-text");
        status.getStyleClass().add("status");
        btnStop.getStyleClass().add("stop");

        // Slider ticks & dynamic tooltip
        slSpeed.setShowTickLabels(true);
        slSpeed.setShowTickMarks(true);
        slSpeed.setMajorTickUnit(100);
        slSpeed.setMinorTickCount(4);
        slSpeed.setSnapToTicks(true);
        Tooltip speedTip = tt("Speed: " + (int) slSpeed.getValue() + " ms/step");
        slSpeed.setTooltip(speedTip);
        slSpeed.valueProperty().addListener((o, ov, nv) -> speedTip.setText("Speed: " + nv.intValue() + " ms/step"));

        // Tooltips
        spRows.setTooltip(tt("Número de filas del tablero"));
        spCols.setTooltip(tt("Número de columnas del tablero"));
        spSR.setTooltip(tt("Fila de inicio (0-index)"));
        spSC.setTooltip(tt("Columna de inicio (0-index)"));
        cbMode.setTooltip(tt("""
                single: calcula un único tour (más rápido)
                all: busca todos los tours (puede tardar mucho)
                """));
        chkClosed.setTooltip(tt("Si está marcado, el tour debe volver al inicio (tour cerrado)"));
        cbStrategy.setTooltip(tt("""
                Estrategia de búsqueda:
                • backtrack: backtracking clásico (exhaustivo)
                • warnsdorff: heurística de Warnsdorff (muy rápida)
                • parallel: backtracking paralelo (usa varios hilos)
                """));
        spFork.setTooltip(tt("Profundidad a partir de la cual bifurcar en paralelo"));
        chkUseCustomPool.setTooltip(tt("Usar un pool propio en paralelo (control del nº de hilos)"));
        spPool.setTooltip(tt("Tamaño del pool paralelo (nº máximo de hilos)"));
        tfOut.setTooltip(tt("Directorio de salida para exportar resultados"));
        chkExport.setTooltip(tt("Exportar tour en TXT y JSON"));
        btnRun.setTooltip(tt("Ejecutar búsqueda"));
        btnPause.setTooltip(tt("Pausar/Reanudar animación"));
        btnStop.setTooltip(tt("Detener cálculo/animación actual"));

        // Initial UI state
        btnPause.setDisable(true); // enabled only while animating
        btnStop.setDisable(true); // enabled only while running
        setAnimating(false);

        // Help content & listeners
        cbMode.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshHelp());
        cbStrategy.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshHelp());
        chkClosed.selectedProperty().addListener((o, a, b) -> refreshHelp());
        refreshHelp();
    }

    // Build the contextual help text
    private void refreshHelp() {
        Map<String, String> modeHelp = Map.of(
                "single", "Finds a single tour and stops. Usually faster.",
                "all", "Enumerates all tours. Can take very long on large boards.");
        Map<String, String> strategyHelp = Map.of(
                "backtrack", "Exhaustive backtracking; guaranteed to find a solution if one exists.",
                "warnsdorff", "Warnsdorff heuristic; very fast in many cases.",
                "parallel", "Parallel backtracking; splits work across threads.");

        String mode = getSelectedMode();
        String strat = getSelectedStrategy();
        boolean closed = isClosed();

        String text = """
                • Mode: %s
                  %s

                • Strategy: %s
                  %s

                • Tour type: %s

                Tips:
                - Large boards + "all" can take a very long time.
                - "parallel" leverages multiple threads; adjust 'Fork depth' and 'Pool size'.
                """.formatted(
                mode, modeHelp.getOrDefault(mode, ""),
                strat, strategyHelp.getOrDefault(strat, ""),
                closed ? "Closed (returns to the start)" : "Open");

        helpText.getChildren().setAll(new Text(text));
        // IMPORTANT: ensure CSS gets applied to the Text nodes
        helpText.getChildren().forEach(n -> n.getStyleClass().add("text"));
    }

    // Layout helpers
    private HBox row(Object leftLabel, Object leftControl, Object rightLabel, Object rightControl) {
        Node l = node(leftLabel);
        Node lc = node(leftControl);
        Node r = node(rightLabel);
        Node rc = node(rightControl);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox h = new HBox(12, l, lc, spacer, r, rc);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private HBox row(Object leftLabel, Object leftControl) {
        Node l = node(leftLabel);
        Node lc = node(leftControl);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox h = new HBox(12, l, lc, spacer);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Node node(Object o) {
        return (o instanceof Node n) ? n : new Label(String.valueOf(o));
    }

    // Public wiring API
    public void setOnRun(Consumer<RunConfig> onRun) {
        this.onRun = onRun;
    }

    /**
     * Called when Pause/Resume is clicked; consumer receives true=paused,
     * false=running.
     */
    public void setOnPauseChanged(Consumer<Boolean> onPauseChanged) {
        this.onPauseChanged = onPauseChanged;
    }

    public void setOnStop(Runnable onStop) {
        this.onStop = onStop;
    }

    public void showMessage(String msg) {
        status.setText(msg);
    }

    // Expose selection as getters + observable properties (if needed externally)
    public String getSelectedMode() {
        return cbMode.getValue();
    }

    public String getSelectedStrategy() {
        return cbStrategy.getValue();
    }

    public boolean isClosed() {
        return chkClosed.isSelected();
    }

    public javafx.beans.property.ReadOnlyObjectProperty<String> selectedModeProperty() {
        return cbMode.getSelectionModel().selectedItemProperty();
    }

    public javafx.beans.property.ReadOnlyObjectProperty<String> selectedStrategyProperty() {
        return cbStrategy.getSelectionModel().selectedItemProperty();
    }

    public javafx.beans.property.BooleanProperty closedProperty() {
        return chkClosed.selectedProperty();
    }

    // UI state helpers
    /** Enables/disables the Run button and updates the status label. */
    public void setRunning(boolean running) {
        btnRun.setDisable(running);
        status.setText(running ? "Running..." : "Ready");
        btnStop.setDisable(!running); // Stop only makes sense when something is running
    }

    /** Enables/disables Pause button; resets its label when disabling. */
    public void setAnimating(boolean animating) {
        btnPause.setDisable(!animating);
        if (!animating) {
            paused = false;
            btnPause.setText("Pause");
        }
    }

    /** Programmatically set pause state and label. */
    public void setPaused(boolean paused) {
        this.paused = paused;
        btnPause.setText(paused ? "Resume" : "Pause");
    }

    // Build current config snapshot
    private RunConfig currentConfig() {
        int rows = spRows.getValue();
        int cols = spCols.getValue();
        return new RunConfig(
                rows, cols,
                spSR.getValue(), spSC.getValue(),
                cbMode.getValue(), cbStrategy.getValue(),
                chkClosed.isSelected(),
                spFork.getValue(),
                chkUseCustomPool.isSelected() ? spPool.getValue() : null,
                (int) slSpeed.getValue(),
                chkExport.isSelected(),
                tfOut.getText());
    }

    // Data record
    public record RunConfig(
            int rows, int cols,
            int startRow, int startCol,
            String mode, String strategy,
            boolean closed,
            int forkDepth,
            Integer poolParallelism,
            int msPerStep,
            boolean export,
            String exportDir) {

        public Map<String, Object> metadata(String modeEffective) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("rows", rows);
            m.put("cols", cols);
            m.put("startRow", startRow);
            m.put("startCol", startCol);
            m.put("tourType", closed ? "closed" : "open");
            m.put("mode", modeEffective.toLowerCase(Locale.ROOT));
            m.put("strategy", strategy.toLowerCase(Locale.ROOT));
            if ("parallel".equalsIgnoreCase(strategy)) {
                m.put("forkDepth", forkDepth);
                m.put("pool", (poolParallelism == null) ? "common" : poolParallelism);
            }
            m.put("timestamp", ts);
            return m;
        }
    }

    // Small UX helpers
    private static Tooltip tt(String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(javafx.util.Duration.millis(200));
        t.setHideDelay(javafx.util.Duration.ZERO);
        t.setShowDuration(javafx.util.Duration.seconds(30));
        return t;
    }

    private static <T> void enableScroll(Spinner<T> sp) {
        sp.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, ev -> {
            if (ev.getDeltaY() > 0)
                sp.increment();
            else
                sp.decrement();
            ev.consume();
        });
    }

    private static void selectAllOnFocus(Spinner<?> sp) {
        var tf = sp.getEditor();
        tf.focusedProperty().addListener((o, oldV, now) -> {
            if (now)
                Platform.runLater(tf::selectAll);
        });
    }

    private static void centerText(Spinner<?> sp) {
        sp.getEditor().setAlignment(Pos.CENTER);
    }
}
