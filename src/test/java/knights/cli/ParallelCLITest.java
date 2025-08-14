package knights.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelCLITest {

    @Test
    void testParallelSingleMode() {
        String[] args = { "5", "5", "0", "0", "single", "open", "parallel" };

        // Capturar salida de consola
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        knights.Main.main(args);

        String output = outContent.toString();

        assertTrue(output.contains("Strategy: parallel"), "Debe indicar estrategia parallel");
        assertTrue(output.contains("("), "Debe imprimir posiciones del tour");
    }

    @Test
    void testParallelAllMode() {
        String[] args = { "5", "5", "0", "0", "all", "open", "parallel" };

        // Capturar salida de consola
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        knights.Main.main(args);

        String output = outContent.toString();

        assertTrue(output.contains("Found"), "Debe indicar cuántas soluciones encontró");
        assertTrue(output.contains("Solution #"), "Debe imprimir al menos una solución");
    }
}
