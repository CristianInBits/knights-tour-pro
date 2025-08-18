package knights.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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
    }

    public void initGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        getChildren().clear();
        cells = new Cell[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = new Cell(((r + c) % 2 == 0) ? Color.web("#fafafa") : Color.web("#d0d0d0"));
                cells[r][c] = cell;
                add(cell.root, c, r);
                GridPane.setHalignment(cell.root, HPos.CENTER);
                GridPane.setValignment(cell.root, VPos.CENTER);
            }
        }

        // Ajuste responsive: cada celda del mismo tamaño dentro del grid
        widthProperty().addListener((obs, ov, nv) -> resizeCells());
        heightProperty().addListener((obs, ov, nv) -> resizeCells());
        resizeCells();
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

            KeyFrame kf = new KeyFrame(Duration.millis(index * msPerStep), e -> {
                Cell cell = cells[sr][sc];
                cell.mark(index + 1);
            });
            timeline.getKeyFrames().add(kf);
            step++;
        }
        timeline.setCycleCount(1);
        timeline.playFromStart();
    }

    private void resizeCells() {
        if (cells == null || rows == 0 || cols == 0)
            return;
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        double cellSize = Math.floor(Math.min((w - (cols - 1)) / cols, (h - (rows - 1)) / rows));

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                cells[r][c].resize(cellSize);
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
            rect.setArcWidth(8);
            rect.setArcHeight(8);
            rect.setFill(base);
            rect.setStroke(Color.web("#888"));
            root.getChildren().addAll(rect, text);
        }

        void resize(double size) {
            rect.setWidth(size);
            rect.setHeight(size);
            text.setStyle("-fx-font-size: " + Math.max(12, (int) (size * 0.33)) + "px; -fx-font-weight: bold;");
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
