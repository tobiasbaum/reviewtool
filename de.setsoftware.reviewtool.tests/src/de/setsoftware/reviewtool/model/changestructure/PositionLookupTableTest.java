package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.Test;

/**
 * Tests for {@link PositionLookupTable}.
 */
public class PositionLookupTableTest {

    private static PositionInText pos(int line, int column) {
        return new PositionInText(line, column);
    }

    @Test
    public void testTransformPosition() throws Exception {
        final PositionLookupTable t = PositionLookupTable.create(new StringReader(
                "zeile 1\r\n"
                        + "zeile 2\r\n"
                        + "zeile laenger\r\n"
                        + "letzte zeile"));

        assertEquals(0, t.getCharsSinceFileStart(pos(1, 1)));
        assertEquals(2, t.getCharsSinceFileStart(pos(1, 3)));
        assertEquals(9, t.getCharsSinceFileStart(pos(2, 1)));
        assertEquals(18, t.getCharsSinceFileStart(pos(3, 1)));
        assertEquals(33, t.getCharsSinceFileStart(pos(4, 1)));
    }

    @Test
    public void testWithInvalidPositions() throws Exception {
        final PositionLookupTable t = PositionLookupTable.create(new StringReader(
                "zeile 1\r\n"
                        + "zeile 2\r\n"
                        + "zeile laenger\r\n"
                        + "letzte zeile"));

        assertEquals(0, t.getCharsSinceFileStart(pos(0, 1)));
        assertEquals(0, t.getCharsSinceFileStart(pos(0, 3)));
        assertEquals(45, t.getCharsSinceFileStart(pos(5, 1)));
        assertEquals(45, t.getCharsSinceFileStart(pos(5, 2)));
        assertEquals(45, t.getCharsSinceFileStart(pos(6, 6)));
    }

}
