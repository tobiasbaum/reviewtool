package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Fragment;

/**
 * Tests for {@link ImportChangeFilter}.
 */
public class ImportChangeFilterTest {

    private static IChange change(String from, String to) {
        return ChangestructureFactory.createTextualChangeHunk(fragment(from), fragment(to), false);
    }

    private static IFragment fragment(String content) {
        return Fragment.createWithContent(
                ChangestructureFactory.createFileInRevision("", null),
                ChangestructureFactory.createPositionInText(1, 1),
                ChangestructureFactory.createPositionInText(2, 1),
                content);
    }

    @Test
    public void testFilterImportDeleted() {
        assertTrue(new ImportChangeFilter().isIrrelevant(change(
                "import some.test.package.Class;\n",
                "")));
    }

    @Test
    public void testFilterImportAdded() {
        assertTrue(new ImportChangeFilter().isIrrelevant(change(
                "",
                "import some.test.package.Class;\n")));
    }

    @Test
    public void testFilterImportsChanged() {
        assertTrue(new ImportChangeFilter().isIrrelevant(change(
                "import some.test.package.Class1;\n"
                + "import some.test.package.Class2;\n",
                "import another.test.package.Class1;\n"
                + "\n"
                + "import yet.another.test.package.Class2;\n")));
    }

    @Test
    public void testDontFilterMoreThanImportsChanged() {
        assertFalse(new ImportChangeFilter().isIrrelevant(change(
                "import some.test.package.Class1;\n"
                + "this is not an import\n",
                "import another.test.package.Class1;\n"
                + "this is still not an import\n")));
    }

    @Test
    public void testDontFilterNoImportsAtAll() {
        assertFalse(new ImportChangeFilter().isIrrelevant(change(
                "public static class Class1 {\n",
                "public static class Class2 {\n")));
    }

    @Test
    public void testDontFilterPureWhitespaceChange() {
        assertFalse(new ImportChangeFilter().isIrrelevant(change(
                "",
                "\n")));
    }

}
