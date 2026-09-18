package knights.ui;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The side panel: everything the user sets before a run, stacked in one column.
 *
 * Controls are grouped by what they affect (board, search, animation, output) with a
 * label above each field rather than beside it, which keeps the column narrow enough to
 * leave the board the rest of the window.
 */
public class ControlsPane extends VBox {

    private static final double PANEL_WIDTH = 310;

    private final Spinner<Integer> spRows = new Spinner<>(1, 20, 6);
    private final Spinner<Integer> spCols = new Spinner<>(1, 20, 6);
    private final Spinner<Integer> spSR = new Spinner<>(0, 19, 0);
    private final Spinner<Integer> spSC = new Spinner<>(0, 19, 0);
    private final Spinner<Integer> spPool = new Spinner<>(1, 64,
            Math.max(2, Runtime.getRuntime().availableProcessors()));
    private final Spinner<Integer> spFork = new Spinner<>(0, 8, 2);

    private final ChoiceBox<String> cbMode = new ChoiceBox<>();
    private final ChoiceBox<String> cbStrategy = new ChoiceBox<>();

    private final CheckBox chkClosed = new CheckBox("Closed tour");
    private final CheckBox chkUseCustomPool = new CheckBox("Custom thread pool");
    private final CheckBox chkExport = new CheckBox("Write TXT and JSON");

    private final Slider slSpeed = new Slider(10, 400, 80); // ms per step
    private final Label lblSpeed = new Label("80 ms");
    private final TextField tfOut = new TextField("output");

    private final Button btnRun = new Button("Run");
    private final Button btnPause = new Button("Pause");
    private final Button btnStop = new Button("Stop");

    private final Circle statusDot = new Circle(4);
    private final Label statusText = new Label("Ready");

    private final TextFlow hint = new TextFlow();

    // Rows that only matter for the parallel strategy, hidden otherwise.
    private final VBox parallelOptions = new VBox(10);

    private Consumer<RunConfig> onRun;
    private Consumer<Boolean> onPauseChanged;
    private Runnable onStop;

    private boolean paused = false;

    public ControlsPane() {
        getStyleClass().add("sidebar");
        setPrefWidth(PANEL_WIDTH);
        setMinWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setSpacing(0);

        configureData();
        configureBehaviour();
        styleInputs();

        getChildren().addAll(
                header(),
                gap(16),
                section("BOARD"),
                twoUp(labelled("Rows", spRows), labelled("Columns", spCols)),
                gap(10),
                twoUp(labelled("Start row", spSR), labelled("Start column", spSC)),
                gap(14),
                divider(),
                gap(13),
                section("SEARCH"),
                labelled("Mode", cbMode),
                gap(10),
                labelled("Strategy", cbStrategy),
                gap(12),
                chkClosed,
                gap(12),
                parallelOptions,
                gap(13),
                divider(),
                gap(13),
                section("ANIMATION"),
                speedRow(),
                slSpeed,
                gap(14),
                divider(),
                gap(13),
                section("OUTPUT"),
                chkExport,
                gap(10),
                tfOut,
                gap(12),
                hintBox(),
                grower(),
                actions(),
                gap(10),
                statusRow());

        refreshHint();
        setAnimating(false);
        btnStop.setDisable(true);
    }

    // ===== Layout helpers =====

    private Node header() {
        Label title = new Label("Knight's Tour");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Four strategies, one board");
        subtitle.getStyleClass().add("app-subtitle");
        VBox box = new VBox(2, title, subtitle);
        return box;
    }

    private Node section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        VBox box = new VBox(l);
        box.setPadding(new Insets(0, 0, 7, 0));
        return box;
    }

    /** A field with its name above it, filling the width it is given. */
    private VBox labelled(String name, Region control) {
        Label l = new Label(name);
        l.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(5, l, control);
        return box;
    }

    /** Two fields side by side, each taking half the panel. */
    private Node twoUp(Region left, Region right) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        half.setHalignment(HPos.LEFT);
        ColumnConstraints half2 = new ColumnConstraints();
        half2.setPercentWidth(50);
        half2.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().addAll(half, half2);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        return grid;
    }

    private Node speedRow() {
        Label name = new Label("Speed");
        name.getStyleClass().add("field-label");
        lblSpeed.getStyleClass().add("value-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(name, spacer, lblSpeed);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 4, 0));
        return row;
    }

    private Node hintBox() {
        hint.getStyleClass().add("hint");
        hint.setMaxWidth(Double.MAX_VALUE);
        hint.setLineSpacing(3);
        return hint;
    }

    private Node actions() {
        btnRun.getStyleClass().add("primary");
        btnStop.getStyleClass().add("danger");
        btnRun.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnRun, Priority.ALWAYS);
        HBox row = new HBox(8, btnRun, btnPause, btnStop);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node statusRow() {
        statusDot.getStyleClass().add("status-dot");
        statusText.getStyleClass().add("status-text");
        HBox row = new HBox(8, statusDot, statusText);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node divider() {
        Region r = new Region();
        r.getStyleClass().add("divider");
        r.setMaxWidth(Double.MAX_VALUE);
        return r;
    }

    private Node gap(double height) {
        Region r = new Region();
        r.setMinHeight(height);
        r.setPrefHeight(height);
        return r;
    }

    /** Pushes the buttons to the bottom of the panel. */
    private Node grower() {
        Region r = new Region();
        r.setMinHeight(8);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    // ===== Wiring =====

    private void configureData() {
        cbMode.getItems().addAll("single", "all");
        cbMode.getSelectionModel().select("single");
        cbStrategy.getItems().addAll("backtrack", "warnsdorff", "parallel");
        cbStrategy.getSelectionModel().select("backtrack");

        chkExport.setSelected(false);
        chkUseCustomPool.setSelected(false);

        parallelOptions.getChildren().addAll(
                twoUp(labelled("Fork depth", spFork), labelled("Pool size", spPool)),
                chkUseCustomPool);
        setParallelOptionsVisible(false);
        spPool.setDisable(true);
    }

    private void configureBehaviour() {
        cbStrategy.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            boolean isParallel = "parallel".equalsIgnoreCase(b);
            setParallelOptionsVisible(isParallel);
            spPool.setDisable(!isParallel || !chkUseCustomPool.isSelected());
            refreshHint();
        });
        chkUseCustomPool.selectedProperty().addListener(
                (o, a, b) -> spPool.setDisable(!b || !"parallel".equalsIgnoreCase(cbStrategy.getValue())));
        cbMode.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> refreshHint());
        chkClosed.selectedProperty().addListener((o, a, b) -> refreshHint());

        // Keep the starting square inside the board when it shrinks
        spRows.valueProperty().addListener((o, ov, nv) -> spSR.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, nv - 1, Math.min(spSR.getValue(), nv - 1))));
        spCols.valueProperty().addListener((o, ov, nv) -> spSC.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, nv - 1, Math.min(spSC.getValue(), nv - 1))));

        slSpeed.valueProperty().addListener((o, ov, nv) -> lblSpeed.setText(nv.intValue() + " ms"));

        btnRun.setOnAction(e -> {
            if (onRun != null) {
                onRun.accept(currentConfig());
            }
        });
        btnPause.setOnAction(e -> {
            paused = !paused;
            btnPause.setText(paused ? "Resume" : "Pause");
            if (onPauseChanged != null) {
                onPauseChanged.accept(paused);
            }
        });
        btnStop.setOnAction(e -> {
            if (onStop != null) {
                onStop.run();
            }
        });

        tooltips();
    }

    private void setParallelOptionsVisible(boolean visible) {
        parallelOptions.setVisible(visible);
        parallelOptions.setManaged(visible); // collapse the space when hidden
    }

    private void styleInputs() {
        Stream.of(spRows, spCols, spSR, spSC, spFork, spPool).forEach(sp -> {
            sp.setEditable(true);
            enableScroll(sp);
            selectAllOnFocus(sp);
            commitOnFocusLoss(sp);
        });
        cbMode.setMaxWidth(Double.MAX_VALUE);
        cbStrategy.setMaxWidth(Double.MAX_VALUE);
        cbMode.setPrefHeight(32);
        cbStrategy.setPrefHeight(32);
        slSpeed.setMaxWidth(Double.MAX_VALUE);
    }

    private void tooltips() {
        spRows.setTooltip(tt("Número de filas del tablero"));
        spCols.setTooltip(tt("Número de columnas del tablero"));
        spSR.setTooltip(tt("Fila de inicio (empieza en 0)"));
        spSC.setTooltip(tt("Columna de inicio (empieza en 0)"));
        cbMode.setTooltip(tt("""
                single: encuentra un único recorrido y para
                all: enumera todos (puede tardar muchísimo)"""));
        cbStrategy.setTooltip(tt("""
                backtrack: exhaustivo, encuentra solución si existe
                warnsdorff: heurística, la más rápida en abiertos
                parallel: explora varias ramas a la vez, mejor en cerrados"""));
        chkClosed.setTooltip(tt("El recorrido debe terminar a un salto de caballo del inicio"));
        spFork.setTooltip(tt("Hasta qué profundidad se reparte la búsqueda entre hilos"));
        chkUseCustomPool.setTooltip(tt("Fijar el número de hilos en vez de usar el pool compartido"));
        spPool.setTooltip(tt("Número de hilos"));
        tfOut.setTooltip(tt("Carpeta donde se guardan los resultados"));
        chkExport.setTooltip(tt("Exportar el recorrido a TXT y JSON"));
        btnRun.setTooltip(tt("Buscar un recorrido"));
        btnPause.setTooltip(tt("Pausar o reanudar la animación"));
        btnStop.setTooltip(tt("Detener la búsqueda y limpiar el tablero"));
        slSpeed.setTooltip(tt("Milisegundos entre paso y paso de la animación"));
    }

    /** One short line about the current selection, updated as it changes. */
    private void refreshHint() {
        String strategy = getSelectedStrategy();
        boolean closed = isClosed();
        String text;

        if ("warnsdorff".equals(strategy)) {
            text = closed
                    ? "Warnsdorff rarely closes a tour: it never backtracks, so it often ends up with no answer. Try parallel."
                    : "The fastest option for open tours, and it needs no threads.";
        } else if ("parallel".equals(strategy)) {
            text = closed
                    ? "The right choice here. Several opening branches are explored at once, so a dead end costs little."
                    : "Fine, but plain warnsdorff is quicker on open tours — forking only adds overhead.";
        } else {
            text = "all".equals(getSelectedMode())
                    ? "The only strategy that can enumerate every tour. Keep the board small."
                    : "Exhaustive: slower, but it finds a tour whenever one exists.";
        }

        Text node = new Text(text);
        node.getStyleClass().add("hint-text");
        hint.getChildren().setAll(node);
    }

    // ===== Public API =====

    public void setOnRun(Consumer<RunConfig> onRun) {
        this.onRun = onRun;
    }

    /** Called when Pause/Resume is clicked; true means paused. */
    public void setOnPauseChanged(Consumer<Boolean> onPauseChanged) {
        this.onPauseChanged = onPauseChanged;
    }

    public void setOnStop(Runnable onStop) {
        this.onStop = onStop;
    }

    public void showMessage(String msg) {
        statusText.setText(msg);
        statusDot.getStyleClass().removeAll("running", "done", "error");
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.startsWith("error") || lower.contains("no solution")) {
            statusDot.getStyleClass().add("error");
        } else if (lower.startsWith("running")) {
            statusDot.getStyleClass().add("running");
        } else if (lower.startsWith("done") || lower.startsWith("exported")) {
            statusDot.getStyleClass().add("done");
        }
    }

    /** Enables/disables Run and updates the status line. */
    public void setRunning(boolean running) {
        btnRun.setDisable(running);
        btnStop.setDisable(!running);
        showMessage(running ? "Running…" : "Ready");
    }

    /** Enables/disables Pause; resets its label when disabling. */
    public void setAnimating(boolean animating) {
        btnPause.setDisable(!animating);
        if (!animating) {
            paused = false;
            btnPause.setText("Pause");
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        btnPause.setText(paused ? "Resume" : "Pause");
    }

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

    // ===== Config snapshot =====

    private RunConfig currentConfig() {
        return new RunConfig(
                spRows.getValue(), spCols.getValue(),
                spSR.getValue(), spSC.getValue(),
                cbMode.getValue(), cbStrategy.getValue(),
                chkClosed.isSelected(),
                spFork.getValue(),
                chkUseCustomPool.isSelected() ? spPool.getValue() : null,
                (int) slSpeed.getValue(),
                chkExport.isSelected(),
                tfOut.getText());
    }

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

    // ===== Small UX helpers =====

    private static Tooltip tt(String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(javafx.util.Duration.millis(350));
        t.setHideDelay(javafx.util.Duration.ZERO);
        t.setShowDuration(javafx.util.Duration.seconds(30));
        return t;
    }

    private static <T> void enableScroll(Spinner<T> sp) {
        sp.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, ev -> {
            if (ev.getDeltaY() > 0) {
                sp.increment();
            } else {
                sp.decrement();
            }
            ev.consume();
        });
    }

    private static void selectAllOnFocus(Spinner<?> sp) {
        var tf = sp.getEditor();
        tf.focusedProperty().addListener((o, oldV, now) -> {
            if (now) {
                Platform.runLater(tf::selectAll);
            }
        });
    }

    /**
     * An editable Spinner keeps the old value when the user types and clicks away without
     * pressing Enter, so commit the text on focus loss.
     */
    private static void commitOnFocusLoss(Spinner<?> sp) {
        sp.getEditor().focusedProperty().addListener((o, had, has) -> {
            if (!has) {
                sp.increment(0);
            }
        });
    }
}
