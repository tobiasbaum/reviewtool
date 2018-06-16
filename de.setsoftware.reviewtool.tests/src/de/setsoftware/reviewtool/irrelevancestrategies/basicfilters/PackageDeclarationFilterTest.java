package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Fragment;

/**
 * Tests for {@link PackageDeclarationFilter}.
 */
public class PackageDeclarationFilterTest {

    private static IChange change(String from, String to) {
        return ChangestructureFactory.createTextualChangeHunk(null, fragment(from), fragment(to), false);
    }

    private static IFragment fragment(String content) {
        return Fragment.createWithContent(
                ChangestructureFactory.createFileInRevision("", null),
                ChangestructureFactory.createPositionInText(1, 1),
                ChangestructureFactory.createPositionInText(2, 1),
                content);
    }

    @Test
    public void testFilterPackageChanged() {
        assertTrue(new PackageDeclarationFilter().isIrrelevant(change(
                "package some.test.package;\n",
                "package another.test.package;\n")));
    }

    @Test
    public void testDontFilterAddedPackage() {
        assertFalse(new PackageDeclarationFilter().isIrrelevant(change(
                "",
                "package some.test.package;\n")));
    }

    @Test
    public void testDontFilterRemovedPackage() {
        assertFalse(new PackageDeclarationFilter().isIrrelevant(change(
                "package some.test.package;\n",
                "")));
    }

    @Test
    public void testDontFilterMoreThanPackageChanged() {
        assertFalse(new PackageDeclarationFilter().isIrrelevant(change(
                "package some.test.package;\n",
                "package another.test.package;\n"
                + "something more\n")));
    }

    @Test
    public void testDontFilterNoPackageAtAll() {
        assertFalse(new PackageDeclarationFilter().isIrrelevant(change(
                "public static class Class1 {\n",
                "public static class Class2 {\n")));
    }

}
