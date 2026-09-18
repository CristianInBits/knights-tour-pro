package knights.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import knights.model.Position;

import java.util.List;

public class BoardView extends GridPane {

    private int rows = 0, cols = 0;
    private Cell[][] cells;
    /** The last cell that displayed the knight (to hide it when moving). */
    private Cell last;

    private Timeline timeline;

    /**
     * Optional hook: called when the animation finishes (naturally or because it
     * runs out of frames).
     */
    private Runnable onAnimationFinished;

    /** Knight PNG resource (transparent background recommended). */
    private static final String KNIGHT_RESOURCE = "/knight.png";
    private final Image knightImage;

    public BoardView() {
        setHgap(1);
        setVgap(1);
        setStyle("-fx-background-color: #222;");
        setPrefSize(640, 640);
        setMinSize(200, 200);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Robust PNG load; falls back to glyph ♞ if it fails.
        this.knightImage = loadKnightImage();
    }

    /** Builds/rebuilds the grid of cells. */
    public void initGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.last = null; // reset the “current knight” pointer

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
                // Custom brown palette (light/dark)
                Color base = ((r + c) % 2 == 0) ? Color.web("#a07c5e") : Color.web("#7a4323ff");
                Cell cell = new Cell(base, knightImage);
                cells[r][c] = cell;
                add(cell.root, c, r);
                GridPane.setHgrow(cell.root, Priority.ALWAYS);
                GridPane.setVgrow(cell.root, Priority.ALWAYS);
            }
        }
    }

    /** Clears trail/animation. */
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
        last = null;
    }

    /**
     * Plays the tour animation. Leaves numbers as a trail and shows a single
     * knight on the current cell (hiding it from the previous one).
     */
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

                // 1) Leave a trail: color as visited + write step number
                cell.mark(index + 1);

                // 2) Move the knight: hide the old one, show the new one
                if (last != null)
                    last.showKnight(false);
                cell.showKnight(true);
                last = cell;
            });

            timeline.getKeyFrames().add(kf);
            step++;
        }

        timeline.setCycleCount(1);
        timeline.setOnFinished(ev -> {
            if (onAnimationFinished != null)
                onAnimationFinished.run();
        });
        timeline.playFromStart();
    }

    /** Pauses the animation (if running). */
    public void pauseAnimation() {
        if (timeline != null)
            timeline.pause();
    }

    /** Resumes the animation (if paused). */
    public void resumeAnimation() {
        if (timeline != null)
            timeline.play();
    }

    /** Returns true if the timeline exists and is currently paused. */
    public boolean isPaused() {
        return timeline != null && timeline.getStatus() == javafx.animation.Animation.Status.PAUSED;
    }

    /** Sets a callback that will be called when the animation finishes. */
    public void setOnAnimationFinished(Runnable onFinished) {
        this.onAnimationFinished = onFinished;
    }

    // ====================== C E L L ======================

    private static class Cell {
        final StackPane root = new StackPane();
        final Rectangle rect = new Rectangle();

        // Vertical content: knight (image or glyph) on top, step number below
        final VBox content = new VBox(4);
        final ImageView knightView; // may be null if no image
        final Text knightGlyph; // fallback ♞ if no image
        final Text stepText = new Text("");

        final Color base;
        final Color visited = Color.web("#39a55dff");

        Cell(Color base, Image sharedKnight) {
            this.base = base;

            root.setAlignment(Pos.CENTER);
            root.setMinSize(0, 0);
            root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            rect.widthProperty().bind(root.widthProperty());
            rect.heightProperty().bind(root.heightProperty());
            rect.setArcWidth(8);
            rect.setArcHeight(8);
            rect.setFill(base);
            rect.setStroke(null); // no border

            // Knight image or fallback glyph ♞
            if (sharedKnight != null) {
                knightView = new ImageView(sharedKnight);
                knightView.setPreserveRatio(true);
                knightView.setSmooth(true);
                knightView.setCache(true);
                knightView.setCacheHint(CacheHint.SPEED);
                // Knight takes ~60% of the cell’s shorter side
                knightView.fitWidthProperty().bind(root.widthProperty().multiply(0.60));
                knightView.fitHeightProperty().bind(root.heightProperty().multiply(0.60));
                knightGlyph = null;
            } else {
                knightView = null;
                knightGlyph = new Text("♞");
                knightGlyph.setManaged(true);
            }

            stepText.setManaged(true);
            stepText.setFill(Color.WHITE);

            content.setAlignment(Pos.CENTER);
            content.setFillWidth(false);
            if (knightView != null) {
                content.getChildren().addAll(knightView, stepText);
            } else {
                content.getChildren().addAll(knightGlyph, stepText);
            }

            // Dynamic scaling
            root.widthProperty().addListener((o, ov, nv) -> updateFonts());
            root.heightProperty().addListener((o, ov, nv) -> updateFonts());

            // At start, knight is hidden (it appears only on the current cell)
            showKnight(false);

            root.getChildren().addAll(rect, content);
        }

        /** Adjust fonts (and glyph color for contrast on base background). */
        void updateFonts() {
            double side = Math.min(root.getWidth(), root.getHeight());
            double stepSize = Math.max(10, side * 0.22); // step number ≈ 22% of side
            stepText.setFont(Font.font(stepSize));
            // stepText.setStyle("-fx-font-size: " + (int) stepSize + "px; -fx-font-weight: bold;");

            if (knightGlyph != null) {
                double ksize = Math.max(12, side * 0.52); // glyph ≈ 52% of side
                knightGlyph.setStyle("-fx-font-size: " + (int) ksize + "px;");
                // Black on light squares, white on dark squares
                Color glyphColor = (base.getBrightness() > 0.6) ? Color.BLACK : Color.WHITE;
                knightGlyph.setFill(glyphColor);
            }
        }

        /**
         * Leaves a visited color and writes the step number. Does not toggle knight
         * visibility.
         */
        void mark(int step) {
            rect.setFill(visited);
            stepText.setText(Integer.toString(step));
            stepText.setFill(Color.WHITE);
        }

        /** Shows/hides the knight (image or glyph). */
        void showKnight(boolean visible) {
            if (knightView != null)
                knightView.setVisible(visible);
            if (knightGlyph != null)
                knightGlyph.setVisible(visible);
        }

        /** Restores base state. */
        void reset() {
            rect.setFill(base);
            stepText.setText("");
            showKnight(false);
        }
    }

    // ====================== I M A G E L O A D ======================

    /**
     * Synchronous image load with simple diagnostics; returns null on failure (uses
     * glyph fallback).
     */
    private Image loadKnightImage() {
        try {
            var url = getClass().getResource(KNIGHT_RESOURCE);
            System.out.println("[BoardView] Looking for resource: " + KNIGHT_RESOURCE + " -> " + url);
            if (url == null) {
                System.out.println("[BoardView] Resource NOT found on the classpath.");
                return null;
            }
            Image img = new Image(url.toExternalForm(), false);
            if (img.isError()) {
                System.out.println("[BoardView] Error loading image: " + img.getException());
                return null;
            }
            System.out.println("[BoardView] Image loaded. w=" + img.getWidth() + ", h=" + img.getHeight());
            return img;
        } catch (Exception ex) {
            System.out.println("[BoardView] Exception loading image: " + ex);
            return null;
        }
    }
}
