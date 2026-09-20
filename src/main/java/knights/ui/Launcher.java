package knights.ui;

import javafx.application.Application;

/**
 * Starting point for the packaged desktop app.
 *
 * {@link MainFX} cannot be the main class of a runnable JAR. When JavaFX arrives on the
 * classpath rather than the module path — which is what an all-in-one JAR does — the java
 * launcher refuses to start a class that extends {@code Application} and reports "JavaFX
 * runtime components are missing". A class that does not extend it is exempt from that
 * check, so this one starts the UI itself.
 *
 * The command line is unaffected: it still lives in {@link knights.Main}.
 */
public final class Launcher {

    private Launcher() {
        // Prevent instantiation
    }

    public static void main(String[] args) {
        Application.launch(MainFX.class, args);
    }
}
