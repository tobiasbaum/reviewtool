package de.setsoftware.reviewtool.irrelevancestrategies.pathfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Fragment;

/**
 * Tests for {@link PathFilter}.
 */
public class PathFilterTest {

    private static ITextualChange change(String pathFrom, String pathTo) {
        return ChangestructureFactory.createTextualChangeHunk(
                null, FileChangeType.OTHER, fragment(pathFrom), fragment(pathTo));
    }

    private static IFragment fragment(String path) {
        return Fragment.createWithContent(
                ChangestructureFactory.createFileInRevision(path, null),
                ChangestructureFactory.createPositionInText(1, 1),
                ChangestructureFactory.createPositionInText(2, 1),
                "some content");
    }

    @Test
    public void testFilterSimpleIrrelevant() {
        assertTrue(new PathFilter(0, "test123", "a test filter").isIrrelevant(null, change(
                "test123",
                "test123")));
    }

    @Test
    public void testFilterSimpleRelevant() {
        assertFalse(new PathFilter(0, "test123", "a test filter").isIrrelevant(null, change(
                "test456",
                "test456")));
    }

    @Test
    public void testStillRelevantWhenOnlyNewMatches() {
        assertFalse(new PathFilter(0, "test123", "a test filter").isIrrelevant(null, change(
                "test456",
                "test123")));
    }

    @Test
    public void testStillRelevantWhenOnlyOldMatches() {
        assertFalse(new PathFilter(0, "test123", "a test filter").isIrrelevant(null, change(
                "test123",
                "test456")));
    }

    @Test
    public void testSimpleWildcard() {
        final PathFilter filter = new PathFilter(0, "test/*/hallo.txt", "a test filter");
        assertTrue(filter.isIrrelevant(null, change("test/abc/hallo.txt", "test/abc/hallo.txt")));
        assertTrue(filter.isIrrelevant(null, change("test/d/hallo.txt", "test/d/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("test/d/e/hallo.txt", "test/d/e/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("d/hallo.txt", "d/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("test/abc/holla.txt", "test/abc/holla.txt")));
    }

    @Test
    public void testWithSlashesInversed() {
        final PathFilter filter = new PathFilter(0, "test\\*\\hallo.txt", "a test filter");
        assertTrue(filter.isIrrelevant(null, change("test/abc/hallo.txt", "test/abc/hallo.txt")));
        assertTrue(filter.isIrrelevant(null, change("test/d/hallo.txt", "test/d/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("test/d/e/hallo.txt", "test/d/e/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("d/hallo.txt", "d/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("test/abc/holla.txt", "test/abc/holla.txt")));
    }

    @Test
    public void testDoubleWildcard() {
        final PathFilter filter = new PathFilter(0, "test/**/hallo.txt", "a test filter");
        assertTrue(filter.isIrrelevant(null, change("test/abc/hallo.txt", "test/abc/hallo.txt")));
        assertTrue(filter.isIrrelevant(null, change("test/d/hallo.txt", "test/d/hallo.txt")));
        assertTrue(filter.isIrrelevant(null, change("test/d/e/hallo.txt", "test/d/e/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("d/hallo.txt", "d/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("test/abc/holla.txt", "test/abc/holla.txt")));
    }

    @Test
    public void testWildcardInName() {
        final PathFilter filter = new PathFilter(0, "test/**/*.txt", "a test filter");
        assertTrue(filter.isIrrelevant(null, change("test/abc/hallo.txt", "test/abc/hallo.txt")));
        assertTrue(filter.isIrrelevant(null, change("test/d/hallo.txt", "test/d/hallo.txt")));
        assertTrue(filter.isIrrelevant(null, change("test/d/e/hallo.txt", "test/d/e/hallo.txt")));
        assertTrue(filter.isIrrelevant(null, change("test/d/e/fgh/hallo.txt", "test/d/e/FGH/hallo.txt")));
        assertFalse(filter.isIrrelevant(null, change("d/hallo.txt", "d/hallo.txt")));
        assertTrue(filter.isIrrelevant(null, change("test/abc/holla.txt", "test/abc/holla.txt")));
        assertFalse(filter.isIrrelevant(null, change("test/abc/holla_txt", "test/abc/holla_txt")));
    }

    @Test
    public void testWildcardAtStart() {
        final PathFilter filter = new PathFilter(0, "**/*.exe", "a test filter");
        assertTrue(filter.isIrrelevant(null, change("test/abc/hallo.exe", "test/abc/hallo.exe")));
        assertTrue(filter.isIrrelevant(null, change("test/hallo.exe", "test/hallo.exe")));
        assertTrue(filter.isIrrelevant(null, change("hallo.exe", "hallo.exe")));
        assertTrue(filter.isIrrelevant(null, change("holla.exe", "holla.exe")));
        assertFalse(filter.isIrrelevant(null, change("holla.txt", "holla.txt")));
    }
}
