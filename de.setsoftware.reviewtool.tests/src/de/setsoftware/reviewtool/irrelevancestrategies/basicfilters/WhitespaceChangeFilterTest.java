package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.setsoftware.reviewtool.model.changestructure.Change;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Fragment;

public class WhitespaceChangeFilterTest {

    private static Change change(String from, String to) {
        return ChangestructureFactory.createTextualChangeHunk(fragment(from), fragment(to), false);
    }

    private static Fragment fragment(String content) {
        return ChangestructureFactory.createFragment(null, null, null, content);
    }

    @Test
    public void testFilterChangedIndentation() {
        assertTrue(new WhitespaceChangeFilter().isIrrelevant(change(
                "    abc\n"
                + "    def\n"
                + "    xyz\n",
                "        abc\n"
                + "        def\n"
                + "        xyz\n")));
    }

    @Test
    public void testFilterTabsToSpaces() {
        assertTrue(new WhitespaceChangeFilter().isIrrelevant(change(
                "    abc\n"
                + "    def\n"
                + "    xyz\n",
                "\tabc\n"
                + "\tdef\n"
                + "\txyz\n")));
    }

    @Test
    public void testFilterDifferentEol() {
        assertTrue(new WhitespaceChangeFilter().isIrrelevant(change(
                "abc\r\n"
                + "def\r\n",
                "abc\n"
                + "def\n")));
    }

    @Test
    public void testFilterTrailingSpaces() {
        assertTrue(new WhitespaceChangeFilter().isIrrelevant(change(
                "abc    ",
                "abc")));
    }

    @Test
    public void testFilterRemoveDuplicateSpace() {
        assertTrue(new WhitespaceChangeFilter().isIrrelevant(change(
                "int  a;",
                "int a;")));
    }

    @Test
    public void testDontFilterRealChange() {
        assertFalse(new WhitespaceChangeFilter().isIrrelevant(change(
                "int a;",
                "long a;")));
    }

}
