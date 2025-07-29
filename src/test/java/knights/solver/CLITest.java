package knights.solver;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

import knights.Main;

class CLITest {

    @Test
    void testMainWarnsdorffSingleOpen() {
        String[] args = { "8", "8", "0", "0", "single", "open", "warnsdorff" };

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        Main.main(args);

        System.setOut(originalOut);
        String output = outContent.toString();

        assertTrue(output.contains("Board: 8x8"));
        assertTrue(output.contains("Strategy: warnsdorff"));
        assertTrue(output.contains("0") || output.contains("."), "Should print a board or solution");
    }

    @Test
    void testMainBacktrackAllOpen() {
        String[] args = { "5", "5", "0", "0", "all", "open", "backtrack" };

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        Main.main(args);

        System.setOut(originalOut);
        String output = outContent.toString();

        assertTrue(output.contains("Found"));
        assertTrue(output.contains("Solution #1"));
        assertTrue(output.contains("0") || output.contains("."), "Should print a board or solution");
    }

    @Test
    void testMainBacktrackSingleClosedNoSolution() {
        String[] args = { "5", "5", "0", "0", "single", "closed", "backtrack" };

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        Main.main(args);

        System.setOut(originalOut);
        String output = outContent.toString();

        assertTrue(output.contains("No solution found."));
    }
}
