package de.setsoftware.reviewtool.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link Position} and its subclasses.
 */
public class PositionTest {

    @Test
    public void testParseGlobal() {
        final Position p = Position.parse("");
        assertEquals(new GlobalPosition(), p);
    }

    @Test
    public void testParseFile() {
        final Position p = Position.parse("(asdf)");
        assertEquals(new FilePosition("asdf"), p);
    }

    @Test
    public void testParseFileLine() {
        final Position p = Position.parse("(asdf, 100)");
        assertEquals(new FileLinePosition("asdf", 100), p);
    }

}
