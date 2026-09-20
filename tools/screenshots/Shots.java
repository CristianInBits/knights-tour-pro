import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Takes the README screenshots from the real interface instead of rebuilding it: starts
 * MainFX, moves the window off screen, drives the controls through the scene graph and
 * snapshots the scene at 2x so the images stay sharp.
 */
public final class Shots {

    private static final double SCALE = 2.0;
    private static final String BACKGROUND = "#0e1014";
    private static final String NL = System.lineSeparator();

    private static Stage stage;
    private static Scene scene;

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : ".";

        new Thread(() -> Application.launch(knights.ui.MainFX.class), "fx-launch").start();
        awaitWindow();

        onFx(() -> {
            stage.setX(-4000);   // off screen, but it still renders
            stage.setY(60);
            // Tall enough that the sidebar never scrolls, even when the parallel
            // strategy adds its own two controls.
            stage.setWidth(1180);
            stage.setHeight(980);
        });
        Thread.sleep(1500);

        inventory();

        shoot(out + "/ui-ready.png");
        System.out.println("-> ui-ready.png");

        configure(8, 8, "warnsdorff", false, 10);
        Thread.sleep(400);
        run();
        awaitIdle(30000);
        Thread.sleep(800);
        shoot(out + "/ui-tour.png");
        System.out.println("-> ui-tour.png");

        configure(6, 6, "parallel", true, 10);
        Thread.sleep(400);
        run();
        awaitIdle(30000);
        Thread.sleep(800);
        shoot(out + "/ui-closed.png");
        System.out.println("-> ui-closed.png");

        Platform.exit();
        System.exit(0);
    }

    // ===== driving the interface =====

    private static void configure(int rows, int cols, String strategy, boolean closed, int speedMs) {
        onFx(() -> {
            List<Node> spinners = find(Spinner.class);
            setSpinner(spinners.get(0), rows);
            setSpinner(spinners.get(1), cols);

            List<Node> combos = find(ChoiceBox.class);
            selectValue(combos.get(1), strategy);

            for (Node n : find(CheckBox.class)) {
                CheckBox cb = (CheckBox) n;
                String text = cb.getText() == null ? "" : cb.getText();
                if (text.startsWith("Closed")) cb.setSelected(closed);
                if (text.startsWith("Write")) cb.setSelected(false);
            }

            ((Slider) find(Slider.class).get(0)).setValue(speedMs);
        });
    }

    private static void run() {
        onFx(() -> {
            for (Node n : find(Button.class)) {
                if ("Run".equals(((Button) n).getText())) {
                    ((Button) n).fire();
                    return;
                }
            }
            throw new IllegalStateException("no Run button in the scene");
        });
    }

    /** Waits until the status line stops reporting a search or an animation in progress. */
    private static void awaitIdle(long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            AtomicReference<String> status = new AtomicReference<>("");
            onFx(() -> {
                for (Node n : find(Label.class)) {
                    if (n.getStyleClass().contains("status-text")) {
                        status.set(((Label) n).getText());
                    }
                }
            });
            String s = status.get();
            if (s.startsWith("Done") || s.startsWith("Exported") || s.startsWith("No solution")
                    || s.startsWith("Error")) {
                System.out.println("   estado: " + s);
                return;
            }
            Thread.sleep(200);
        }
        System.out.println("   AVISO: se agoto la espera");
    }

    @SuppressWarnings("unchecked")
    private static void selectValue(Node combo, String value) {
        ((ChoiceBox<Object>) combo).getSelectionModel().select(value);
    }

    private static void setSpinner(Node node, int value) {
        @SuppressWarnings("unchecked")
        SpinnerValueFactory<Integer> factory =
                (SpinnerValueFactory<Integer>) ((Spinner<?>) node).getValueFactory();
        factory.setValue(value);
    }

    // ===== snapshot =====

    private static void shoot(String path) throws Exception {
        int[] size = new int[2];
        AtomicReference<int[]> pixels = new AtomicReference<>();

        onFx(() -> {
            int w = (int) Math.round(scene.getWidth() * SCALE);
            int h = (int) Math.round(scene.getHeight() * SCALE);
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(Transform.scale(SCALE, SCALE));
            params.setFill(Color.web(BACKGROUND));

            WritableImage image = new WritableImage(w, h);
            scene.getRoot().snapshot(params, image);

            int[] argb = new int[w * h];
            image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
            pixels.set(argb);
            size[0] = w;
            size[1] = h;
        });

        BufferedImage out = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, size[0], size[1], pixels.get(), 0, size[0]);
        ImageIO.write(out, "png", new File(path));
    }

    // ===== plumbing =====

    private static void awaitWindow() throws Exception {
        for (int i = 0; i < 150 && scene == null; i++) {
            Thread.sleep(200);
            try {
                onFx(() -> {
                    for (Window w : Window.getWindows()) {
                        if (w instanceof Stage s && s.isShowing() && s.getScene() != null) {
                            stage = s;
                            scene = s.getScene();
                        }
                    }
                });
            } catch (IllegalStateException toolkitNotUpYet) {
                // keep waiting
            }
        }
        if (scene == null) throw new IllegalStateException("the window never appeared");
    }

    /** Prints what the scene graph walk found, so a wrong control order shows up here. */
    private static void inventory() {
        onFx(() -> {
            StringBuilder sb = new StringBuilder("controles encontrados:" + NL);
            List<Node> spinners = find(Spinner.class);
            for (int i = 0; i < spinners.size(); i++) {
                sb.append("  spinner[").append(i).append("] = ")
                  .append(((Spinner<?>) spinners.get(i)).getValue()).append(NL);
            }
            for (Node n : find(ChoiceBox.class)) {
                sb.append("  choice = ").append(((ChoiceBox<?>) n).getItems()).append(NL);
            }
            for (Node n : find(CheckBox.class)) {
                sb.append("  check = ").append(((CheckBox) n).getText()).append(NL);
            }
            for (Node n : find(Button.class)) {
                sb.append("  button = ").append(((Button) n).getText()).append(NL);
            }
            System.out.print(sb);
        });
    }

    private static List<Node> find(Class<?> type) {
        List<Node> found = new ArrayList<>();
        collect(scene.getRoot(), type, found);
        return found;
    }

    private static void collect(Node node, Class<?> type, List<Node> out) {
        if (type.isInstance(node)) {
            out.add(node);
            return;   // do not descend into the skin of a control
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collect(child, type, out);
            }
        }
    }

    private static void onFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (RuntimeException e) {
                failure.set(e);
            } finally {
                done.countDown();
            }
        });
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        if (failure.get() != null) throw failure.get();
    }
}
