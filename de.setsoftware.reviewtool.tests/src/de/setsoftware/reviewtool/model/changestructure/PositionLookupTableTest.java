package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.Test;

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

        assertEquals(1, t.getCharsSinceFileStart(pos(1, 1)));
        assertEquals(3, t.getCharsSinceFileStart(pos(1, 3)));
        assertEquals(10, t.getCharsSinceFileStart(pos(2, 1)));
    }

}
