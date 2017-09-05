package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.setsoftware.reviewtool.model.api.IFragment;

/**
 * Tests for {@link Tour}.
 */
public class TourTest {

    private static PositionInText pos(int line, int column) {
        return new PositionInText(line, column);
    }

    private static FileInRevision file(String name, int revision) {
        return new FileInRevision(name, new RepoRevision(revision, StubRepo.INSTANCE));
    }

    @Test
    public void testCannotMergeNeighboringStopsWithDifferentRelevance() {
        final IFragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(2, 1));
        final IFragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(2, 1));
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(2, 1));
        final Stop s1 = new Stop(new TextualChangeHunk(from1, to1, false, true), current1);

        final IFragment from2 = new Fragment(file("a.java", 2), pos(2, 1), pos(3, 1));
        final IFragment to2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 1));
        final Fragment current2 = new Fragment(file("a.java", 3), pos(2, 1), pos(3, 1));
        final Stop s2 = new Stop(new TextualChangeHunk(from2, to2, true, true), current2);

        assertFalse(s1.canBeMergedWith(s2));
        assertFalse(s2.canBeMergedWith(s1));
    }

    @Test
    public void testCanMergeOverlappingStopsWithDifferentRelevance() {
        final IFragment from1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        final IFragment to1 = new Fragment(file("a.java", 2), pos(1, 1), pos(3, 1));
        final Fragment current1 = new Fragment(file("a.java", 3), pos(1, 1), pos(3, 1));
        final Stop s1 = new Stop(new TextualChangeHunk(from1, to1, false, true), current1);

        final IFragment from2 = new Fragment(file("a.java", 2), pos(2, 1), pos(4, 1));
        final IFragment to2 = new Fragment(file("a.java", 3), pos(2, 1), pos(4, 1));
        final Fragment current2 = new Fragment(file("a.java", 3), pos(2, 1), pos(4, 1));
        final Stop s2 = new Stop(new TextualChangeHunk(from2, to2, true, true), current2);

        assertTrue(s1.canBeMergedWith(s2));
        assertTrue(s2.canBeMergedWith(s1));
    }

}
