package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Fragment;

/**
 * Tests for {@link WhitespaceChangeFilter}.
 */
public class WhitespaceChangeFilterTest {

    private static IChange change(String from, String to) {
        return ChangestructureFactory.createTextualChangeHunk(null, fragment(from), fragment(to));
    }

    private static IFragment fragment(String content) {
        return Fragment.createWithContent(
                ChangestructureFactory.createFileInRevision("", null),
                ChangestructureFactory.createPositionInText(1, 1),
                ChangestructureFactory.createPositionInText(2, 1),
                content);
    }

    @Test
    public void testFilterChangedIndentation() {
        assertTrue(new WhitespaceChangeFilter(0).isIrrelevant(null, change(
                "    abc\n"
                + "    def\n"
                + "    xyz\n",
                "        abc\n"
                + "        def\n"
                + "        xyz\n")));
    }

    @Test
    public void testFilterTabsToSpaces() {
        assertTrue(new WhitespaceChangeFilter(0).isIrrelevant(null, change(
                "    abc\n"
                + "    def\n"
                + "    xyz\n",
                "\tabc\n"
                + "\tdef\n"
                + "\txyz\n")));
    }

    @Test
    public void testFilterDifferentEol() {
        assertTrue(new WhitespaceChangeFilter(0).isIrrelevant(null, change(
                "abc\r\n"
                + "def\r\n",
                "abc\n"
                + "def\n")));
    }

    @Test
    public void testFilterTrailingSpaces() {
        assertTrue(new WhitespaceChangeFilter(0).isIrrelevant(null, change(
                "abc    ",
                "abc")));
    }

    @Test
    public void testFilterRemoveDuplicateSpace() {
        assertTrue(new WhitespaceChangeFilter(0).isIrrelevant(null, change(
                "int  a;",
                "int a;")));
    }

    @Test
    public void testDontFilterRealChange() {
        assertFalse(new WhitespaceChangeFilter(0).isIrrelevant(null, change(
                "int a;",
                "long a;")));
    }

    @Test
    public void testChangeInLineBreaking() {
        assertTrue(new WhitespaceChangeFilter(0).isIrrelevant(null, change(
                "{doStuff(1, 2, 3);}",
                "{\n  doStuff(\n    1,\n    2,\n    3);\n}")));
    }

}
