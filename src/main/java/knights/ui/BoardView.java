package knights.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import knights.model.Position;

import java.util.List;

public class BoardView extends GridPane {

    private int rows = 0, cols = 0;
    private Cell[][] cells;
    private Timeline timeline;

    public BoardView() {
        setHgap(1);
        setVgap(1);
        setStyle("-fx-background-color: #222;");
        setPrefSize(640, 640);
        setMinSize(200, 200);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public void initGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        getChildren().clear();
        getColumnConstraints().clear();
        getRowConstraints().clear();

        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHalignment(HPos.CENTER);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            getColumnConstraints().add(cc);
        }
        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / rows);
            rc.setValignment(VPos.CENTER);
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            getRowConstraints().add(rc);
        }

        cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = new Cell(((r + c) % 2 == 0) ? Color.web("#fafafa") : Color.web("#d0d0d0"));
                cells[r][c] = cell;
                add(cell.root, c, r);
                GridPane.setHgrow(cell.root, Priority.ALWAYS);
                GridPane.setVgrow(cell.root, Priority.ALWAYS);
            }
        }
    }

    public void clearMarks() {
        if (cells == null)
            return;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                cells[r][c].reset();
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    public void animate(List<Position> path, int msPerStep) {
        if (cells == null || path == null || path.isEmpty())
            return;
        if (timeline != null)
            timeline.stop();

        timeline = new Timeline();
        int step = 0;
        for (Position p : path) {
            final int sr = p.row();
            final int sc = p.col();
            final int index = step;

            KeyFrame kf = new KeyFrame(Duration.millis((long) index * msPerStep), e -> {
                Cell cell = cells[sr][sc];
                cell.mark(index + 1);
            });
            timeline.getKeyFrames().add(kf);
            step++;
        }
        timeline.setCycleCount(1);
        timeline.playFromStart();
    }

    // ---- cell ----
    private static class Cell {
        final StackPane root = new StackPane();
        final Rectangle rect = new Rectangle();
        final Text text = new Text("");

        final Color base;
        final Color visited = Color.web("#4f83ff");

        Cell(Color base) {
            this.base = base;

            root.setAlignment(Pos.CENTER);
            root.setMinSize(0, 0);
            root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            rect.widthProperty().bind(root.widthProperty());
            rect.heightProperty().bind(root.heightProperty());
            rect.setArcWidth(8);
            rect.setArcHeight(8);
            rect.setFill(base);
            rect.setStroke(Color.web("#888"));

            text.setManaged(true);
            StackPane.setAlignment(text, Pos.CENTER);

            root.widthProperty().addListener((o, ov, nv) -> updateFont());
            root.heightProperty().addListener((o, ov, nv) -> updateFont());

            root.getChildren().addAll(rect, text);
        }

        void updateFont() {
            double s = Math.max(12, Math.min(root.getWidth(), root.getHeight()) * 0.33);
            text.setStyle("-fx-font-size: " + (int) s + "px; -fx-font-weight: bold;");
        }

        void mark(int step) {
            rect.setFill(visited);
            text.setText(Integer.toString(step));
            text.setFill(Color.WHITE);
        }

        void reset() {
            rect.setFill(base);
            text.setText("");
        }
    }
}
