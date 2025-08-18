package knights.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ControlsPane extends VBox {

    private final Spinner<Integer> spRows = new Spinner<>(1, 20, 6);
    private final Spinner<Integer> spCols = new Spinner<>(1, 20, 6);
    private final Spinner<Integer> spSR = new Spinner<>(0, 19, 0);
    private final Spinner<Integer> spSC = new Spinner<>(0, 19, 0);

    private final ChoiceBox<String> cbMode = new ChoiceBox<>();
    private final ChoiceBox<String> cbStrategy = new ChoiceBox<>();
    private final CheckBox chkClosed = new CheckBox("Closed");

    private final Spinner<Integer> spFork = new Spinner<>(0, 8, 2);
    private final Spinner<Integer> spPool = new Spinner<>(1, 64,
            Math.max(2, Runtime.getRuntime().availableProcessors()));
    private final CheckBox chkUseCustomPool = new CheckBox("Custom Pool");

    private final Slider slSpeed = new Slider(10, 400, 80); // ms per step
    private final Text lblSpeed = new Text("Speed: 80 ms/step");

    private final TextField tfOut = new TextField("output");
    private final CheckBox chkExport = new CheckBox("Export");

    private final Button btnRun = new Button("Run");
    private final Label status = new Label();

    private Consumer<RunConfig> onRun;

    public ControlsPane() {
        setPadding(new Insets(12));
        setSpacing(8);
        getStyleClass().add("controls");

        cbMode.getItems().addAll("single", "all");
        cbMode.getSelectionModel().select("single");

        cbStrategy.getItems().addAll("backtrack", "warnsdorff", "parallel");
        cbStrategy.getSelectionModel().select("parallel");

        chkExport.setSelected(false);
        chkUseCustomPool.setSelected(false);
        spFork.setDisable(true);
        spPool.setDisable(true);

        cbStrategy.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean isParallel = "parallel".equalsIgnoreCase(newV);
            spFork.setDisable(!isParallel);
            chkUseCustomPool.setDisable(!isParallel);
            spPool.setDisable(!isParallel || !chkUseCustomPool.isSelected());
        });
        chkUseCustomPool.selectedProperty().addListener((obs, oldV, newV) -> {
            spPool.setDisable(!newV || !"parallel".equalsIgnoreCase(cbStrategy.getValue()));
        });

        spRows.valueProperty().addListener((o, ov, nv) -> spSR.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, nv - 1, Math.min(spSR.getValue(), nv - 1))));
        spCols.valueProperty().addListener((o, ov, nv) -> spSC.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, nv - 1, Math.min(spSC.getValue(), nv - 1))));

        slSpeed.valueProperty().addListener((obs, ov, nv) -> lblSpeed.setText("Speed: " + nv.intValue() + " ms/step"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);

        int r = 0;
        grid.add(row(new Label("Rows"), spRows, new Label("Cols"), spCols), 0, r++);
        grid.add(row(new Label("Start Row"), spSR, new Label("Start Col"), spSC), 0, r++);
        grid.add(row(new Label("Mode"), cbMode, new Label("Strategy"), cbStrategy), 0, r++);
        grid.add(row(new Label("Closed"), chkClosed, new Label("Fork Depth"), spFork), 0, r++);
        grid.add(row(new Label("Custom Pool"), chkUseCustomPool, new Label("Pool size"), spPool), 0, r++);
        grid.add(row(lblSpeed, slSpeed), 0, r++); // <-- keep Text as Node
        grid.add(row(new Label("Output dir"), tfOut, new Label("Export"), chkExport), 0, r++);

        HBox actions = new HBox(10, btnRun, status);
        actions.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(grid, actions);

        btnRun.setOnAction(e -> {
            RunConfig cfg = currentConfig();
            if (onRun != null) {
                status.setText("Running…");
                onRun.accept(cfg);
            }
        });
    }

    private HBox row(Object leftLabel, Object leftControl, Object rightLabel, Object rightControl) {
        Node l = node(leftLabel);
        Node lc = node(leftControl);
        Node r = node(rightLabel);
        Node rc = node(rightControl);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(12, l, lc, spacer, r, rc);
    }

    private HBox row(Object leftLabel, Object leftControl) {
        Node l = node(leftLabel);
        Node lc = node(leftControl);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(12, l, lc, spacer);
    }

    private Node node(Object o) {
        return (o instanceof Node n) ? n : new Label(String.valueOf(o));
    }

    public void setOnRun(Consumer<RunConfig> onRun) {
        this.onRun = onRun;
    }

    public void showMessage(String msg) {
        status.setText(msg);
    }

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

    public void setRunning(boolean running) {
        btnRun.setDisable(running);
        status.setText(running ? "Running..." : "Ready");
    }

}
