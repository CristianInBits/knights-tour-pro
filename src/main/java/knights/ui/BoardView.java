package knights.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
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
    private static final String KNIGHT_RESOURCE = "/knight2.png";
    private final Image knightImage;

    public BoardView() {
        setHgap(3);
        setVgap(3);
        getStyleClass().add("board");
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
                // Two close shades of slate: the chequer reads clearly without competing
                // with the trail colour that will be painted over it.
                Color base = ((r + c) % 2 == 0) ? Color.web("#242a36") : Color.web("#1a1f28");
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
     * Takes the grid away entirely, back to how things were before the first run.
     *
     * Clearing the marks is not enough on its own: the squares stay in place, and
     * anything drawn behind the board shows through the gaps between them.
     */
    public void clearBoard() {
        clearMarks(); // also stops the timeline
        getChildren().clear();
        getColumnConstraints().clear();
        getRowConstraints().clear();
        cells = null;
        rows = 0;
        cols = 0;
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

            final double progress = (path.size() > 1) ? (double) index / (path.size() - 1) : 0;

            KeyFrame kf = new KeyFrame(Duration.millis((long) index * msPerStep), e -> {
                Cell cell = cells[sr][sc];

                // 1) Leave a trail: colour by progress + write step number
                cell.mark(index + 1, progress);
                if (index == 0) {
                    cell.markAsStart();
                }

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

        /** Ring drawn on the square the knight is standing on, so it is easy to follow. */
        final Rectangle focusRing = new Rectangle();

        // The knight sits in the middle of the square and the step number in a corner,
        // so neither has to give up room for the other.
        final ImageView knightView; // may be null if no image
        final Text knightGlyph; // fallback ♞ if no image
        final Text stepText = new Text("");

        final Color base;

        Cell(Color base, Image sharedKnight) {
            this.base = base;

            root.setAlignment(Pos.CENTER);
            root.setMinSize(0, 0);
            root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

            rect.widthProperty().bind(root.widthProperty());
            rect.heightProperty().bind(root.heightProperty());
            rect.setArcWidth(10);
            rect.setArcHeight(10);
            rect.setFill(base);
            rect.setStroke(null); // no border

            // Knight image or fallback glyph ♞
            if (sharedKnight != null) {
                knightView = new ImageView(sharedKnight);
                knightView.setPreserveRatio(true);
                knightView.setSmooth(true);
                knightView.setCache(true);
                knightView.setCacheHint(CacheHint.SPEED);
                // The knight now has the whole square to itself
                knightView.fitWidthProperty().bind(root.widthProperty().multiply(0.66));
                knightView.fitHeightProperty().bind(root.heightProperty().multiply(0.66));
                knightGlyph = null;
            } else {
                knightView = null;
                knightGlyph = new Text("♞");
            }

            focusRing.widthProperty().bind(root.widthProperty().subtract(5));
            focusRing.heightProperty().bind(root.heightProperty().subtract(5));
            focusRing.setArcWidth(8);
            focusRing.setArcHeight(8);
            focusRing.setFill(Color.TRANSPARENT);
            focusRing.setStroke(Color.web("#ffffff"));
            focusRing.setStrokeWidth(2.5);
            focusRing.setStrokeType(StrokeType.INSIDE);
            focusRing.setVisible(false);
            focusRing.setMouseTransparent(true);

            Node knight = (knightView != null) ? knightView : knightGlyph;

            stepText.setFill(Color.WHITE);
            StackPane.setAlignment(stepText, Pos.TOP_LEFT);

            // Dynamic scaling
            root.widthProperty().addListener((o, ov, nv) -> updateFonts());
            root.heightProperty().addListener((o, ov, nv) -> updateFonts());

            // At start, knight is hidden (it appears only on the current cell)
            showKnight(false);

            root.getChildren().addAll(rect, focusRing, knight, stepText);
        }

        /** Adjust fonts (and glyph color for contrast on base background). */
        void updateFonts() {
            double side = Math.min(root.getWidth(), root.getHeight());
            // Small enough to stay out of the knight's way, and inset from the rounded
            // corner so it never touches the edge.
            double stepSize = Math.max(8, side * 0.155);
            stepText.setFont(Font.font(stepSize));
            double inset = Math.max(3, side * 0.075);
            StackPane.setMargin(stepText, new Insets(inset, 0, 0, inset));

            if (knightGlyph != null) {
                double ksize = Math.max(12, side * 0.52); // glyph ≈ 52% of side
                knightGlyph.setStyle("-fx-font-size: " + (int) ksize + "px;");
                // Black on light squares, white on dark squares
                Color glyphColor = (base.getBrightness() > 0.6) ? Color.BLACK : Color.WHITE;
                knightGlyph.setFill(glyphColor);
            }
        }

        /**
         * Paints the trail and writes the step number. 'progress' runs 0 to 1 across the
         * tour, shifting the colour from indigo to cyan so the order of the moves can be
         * read off the board at a glance. Does not toggle knight visibility.
         */
        void mark(int step, double progress) {
            rect.setFill(trailColour(progress));
            stepText.setText(Integer.toString(step));
            stepText.setFill(Color.web("#f2f4f8").deriveColor(0, 1, 1, 0.72));
        }

        private static Color trailColour(double progress) {
            double t = Math.max(0, Math.min(1, progress));
            return Color.web("#6366f1").interpolate(Color.web("#22d3ee"), t);
        }

        /** Shows/hides the knight (image or glyph) and the ring around it. */
        void showKnight(boolean visible) {
            if (knightView != null)
                knightView.setVisible(visible);
            if (knightGlyph != null)
                knightGlyph.setVisible(visible);
            focusRing.setVisible(visible);
        }

        /**
         * Outlines the square the tour starts from. It stays for the whole run, so a
         * finished board still shows where the knight set off — otherwise the only clue
         * is a small "1" in a corner.
         */
        void markAsStart() {
            rect.setStroke(Color.web("#e8ebf2", 0.6));
            rect.setStrokeWidth(2);
            rect.setStrokeType(StrokeType.INSIDE);
        }

        /** Restores base state. */
        void reset() {
            rect.setFill(base);
            rect.setStroke(null);
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
