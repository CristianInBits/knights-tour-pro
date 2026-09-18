package knights.export;

import knights.model.Board;
import knights.model.Position;
import knights.solver.WarnsdorffSolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SVG export.
 *
 * The important property is that the file actually opens: an SVG that a browser refuses
 * is worse than no SVG, and it is easy to produce one by accident — an unescaped
 * character from the metadata, or a decimal written with a comma under a Spanish locale.
 * So every case parses the result as XML rather than only matching text in it.
 */
class SvgExporterTest {

    private static Map<String, Object> metadata(int rows, int cols) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rows", rows);
        m.put("cols", cols);
        m.put("tourType", "open");
        m.put("strategy", "warnsdorff");
        return m;
    }

    private static List<Position> tourOf(int size) {
        List<Position> tour = new WarnsdorffSolver(new Board(size, size), new Position(0, 0), false).solve();
        assertFalse(tour.isEmpty(), "the test needs a real tour to draw");
        return tour;
    }

    /** Parses the file, failing the test if it is not well-formed XML. */
    private static Document parse(Path file) throws Exception {
        String xml = Files.readString(file);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    // ===== Tests =====

    @Test
    void writesWellFormedSvgForASingleTour(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("tour.svg");
        new SvgExporter().exportSingle(tourOf(6), metadata(6, 6), file.toString());

        Document doc = parse(file);
        assertEquals("svg", doc.getDocumentElement().getLocalName());
        assertEquals("http://www.w3.org/2000/svg", doc.getDocumentElement().getNamespaceURI());
    }

    @Test
    void drawsOneSquarePerCellPlusTheBackground(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("tour.svg");
        new SvgExporter().exportSingle(tourOf(6), metadata(6, 6), file.toString());

        Document doc = parse(file);
        int rects = doc.getElementsByTagName("rect").getLength();
        // 36 squares + the page background + the outline on the starting square
        assertEquals(38, rects, "expected one rect per square, plus background and start outline");
    }

    @Test
    void drawsTheWholePath(@TempDir Path dir) throws Exception {
        List<Position> tour = tourOf(6);
        Path file = dir.resolve("tour.svg");
        new SvgExporter().exportSingle(tour, metadata(6, 6), file.toString());

        Document doc = parse(file);
        assertEquals(tour.size() - 1, doc.getElementsByTagName("line").getLength(),
                "one line per move");
        assertEquals(tour.size(), doc.getElementsByTagName("circle").getLength(),
                "one dot per visited square");
        assertEquals(tour.size(), doc.getElementsByTagName("text").getLength(),
                "one step number per visited square");
    }

    @Test
    void laysOutSeveralToursTogether(@TempDir Path dir) throws Exception {
        List<Position> tour = tourOf(5);
        Path file = dir.resolve("tours.svg");
        new SvgExporter().exportMultiple(List.of(tour, tour, tour), metadata(5, 5), file.toString());

        Document doc = parse(file);
        // 3 boards x 25 squares + background + 3 start outlines
        assertEquals(3 * 25 + 1 + 3, doc.getElementsByTagName("rect").getLength());
        assertEquals(3 * (tour.size() - 1), doc.getElementsByTagName("line").getLength());
    }

    @Test
    void survivesMetadataWithXmlCharacters(@TempDir Path dir) throws Exception {
        // Metadata is free-form; an output path or a label could easily contain these.
        Map<String, Object> hostile = metadata(5, 5);
        hostile.put("note", "a < b & c > d \"quoted\" 'single'");

        Path file = dir.resolve("tour.svg");
        new SvgExporter().exportSingle(tourOf(5), hostile, file.toString());

        Document doc = parse(file); // would throw if the characters leaked through
        assertTrue(doc.getElementsByTagName("desc").item(0).getTextContent().contains("a < b & c > d"));
    }

    @Test
    void worksWithoutAnyMetadata(@TempDir Path dir) throws Exception {
        // Falls back to the size the tour itself covers.
        Path file = dir.resolve("tour.svg");
        new SvgExporter().exportSingle(tourOf(5), null, file.toString());

        Document doc = parse(file);
        assertEquals(25 + 1 + 1, doc.getElementsByTagName("rect").getLength());
    }

    @Test
    void handlesAnEmptyResult(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("tours.svg");
        new SvgExporter().exportMultiple(List.of(), metadata(5, 5), file.toString());

        Document doc = parse(file);
        assertEquals("svg", doc.getDocumentElement().getLocalName());
        assertEquals(0, doc.getElementsByTagName("line").getLength());
    }

    @Test
    void everyLineJoinsTwoSquaresAKnightsMoveApart(@TempDir Path dir) throws Exception {
        // Checks the drawing itself, not just that it parses: each segment has to run
        // between the centres of two squares that a knight could actually move between,
        // which catches a wrong row/column or an off-by-one in the coordinates.
        final int cell = 56;
        final int padding = 20;
        Path file = dir.resolve("tour.svg");
        new SvgExporter().exportSingle(tourOf(6), metadata(6, 6), file.toString());

        Document doc = parse(file);
        var lines = doc.getElementsByTagName("line");
        assertTrue(lines.getLength() > 0);

        for (int i = 0; i < lines.getLength(); i++) {
            var line = (org.w3c.dom.Element) lines.item(i);
            double x1 = Double.parseDouble(line.getAttribute("x1"));
            double y1 = Double.parseDouble(line.getAttribute("y1"));
            double x2 = Double.parseDouble(line.getAttribute("x2"));
            double y2 = Double.parseDouble(line.getAttribute("y2"));

            // Centres sit half a cell in from the edge of their square
            assertEquals(cell / 2.0, x1 % cell, 0.001, "x1 is not on a square centre");
            assertEquals(cell / 2.0, y1 % cell, 0.001, "y1 is not on a square centre");

            int dx = (int) Math.abs(Math.round((x2 - x1) / cell));
            int dy = (int) Math.abs(Math.round((y2 - y1) / cell));
            assertTrue((dx == 1 && dy == 2) || (dx == 2 && dy == 1),
                    "segment " + i + " spans " + dx + "x" + dy + " squares, which is not a knight's move");
        }

        // Nothing may fall outside the drawing area
        double width = Double.parseDouble(doc.getDocumentElement().getAttribute("width"));
        double height = Double.parseDouble(doc.getDocumentElement().getAttribute("height"));
        assertEquals(padding * 2 + 6 * cell, width, 0.001);
        for (int i = 0; i < lines.getLength(); i++) {
            var line = (org.w3c.dom.Element) lines.item(i);
            for (String attr : new String[] { "x1", "x2" }) {
                double v = Double.parseDouble(line.getAttribute(attr));
                assertTrue(v >= 0 && v <= width, attr + " falls outside the canvas");
            }
            for (String attr : new String[] { "y1", "y2" }) {
                double v = Double.parseDouble(line.getAttribute(attr));
                assertTrue(v >= 0 && v <= height, attr + " falls outside the canvas");
            }
        }
    }

    @Test
    void reportsAFailedWrite(@TempDir Path dir) throws IOException {
        Path blocked = dir.resolve("tour.svg");
        Files.createDirectory(blocked);
        assertThrows(IOException.class,
                () -> new SvgExporter().exportSingle(tourOf(5), metadata(5, 5), blocked.toString()));
    }
}
