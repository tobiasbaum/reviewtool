package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests for actions on {link FragmentList}s.
 */
public class FileDiffTest {

    private static FileInRevision file(String name, int revision) {
        return new FileInRevision(name, new RepoRevision(revision), StubRepo.INSTANCE);
    }

    private static PositionInText pos(int line, int col) {
        return new PositionInText(line, col);
    }

    @Test
    public void testAddNonAdjacent1() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 1));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());
    }

    @Test
    public void testAddNonAdjacent2() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(6, 1), pos(7, 1));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final Fragment f3 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 1));
        ff.addFragment(f3);
        ff.coalesce();
        final List<Fragment> expected3 = new ArrayList<>();
        expected3.add(f1);
        expected3.add(f3);
        expected3.add(f2);
        assertEquals(expected3, ff.getFragments());
    }

    @Test
    public void testAddAdjacent1() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 1));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(new Fragment(file("a.java", 1), pos(1, 1), pos(4, 1), f1, f2));
        assertEquals(expected2, ff.getFragments());
    }

    @Test
    public void testAddAdjacent2() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 1));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(new Fragment(file("a.java", 1), pos(1, 1), pos(4, 1), f1, f2));
        assertEquals(expected2, ff.getFragments());
    }

    @Test
    public void testAddAdjacent3() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 1));
        ff.addFragment(f2);
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final Fragment f3 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 1));
        ff.addFragment(f3);
        ff.coalesce();
        final List<Fragment> expected3 = new ArrayList<>();
        expected3.add(new Fragment(file("a.java", 1), pos(1, 1), pos(5, 1), f1, f2, f3));
        assertEquals(expected3, ff.getFragments());
    }

    @Test
    public void testAddAdjacent4() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 1));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final FragmentList ff2 = new FragmentList();
        final Fragment f3 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 1));
        ff2.addFragment(f3);
        final Fragment f4 = new Fragment(file("a.java", 1), pos(5, 1), pos(6, 1));
        ff2.addFragment(f4);
        ff2.coalesce();
        final List<Fragment> expected3 = new ArrayList<>();
        expected3.add(f3);
        expected3.add(f4);
        assertEquals(expected3, ff2.getFragments());

        ff.addFragmentList(ff2);
        ff.coalesce();
        final List<Fragment> expected4 = new ArrayList<>();
        expected4.add(new Fragment(file("a.java", 1), pos(1, 1), pos(6, 1), f1, f2, f3, f4));
        assertEquals(expected4, ff.getFragments());
    }

    @Test
    public void testAddOverlapping1() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        ff.addFragment(f1);
        try {
            ff.addFragment(f1);
            fail("IncompatibleFragmentException expected");
        } catch (final IncompatibleFragmentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testAddOverlapping2() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 1));
        ff.addFragment(f1);
        final Fragment f2 = new Fragment(file("a.java", 1), pos(2, 1), pos(4, 1));
        try {
            ff.addFragment(f2);
            fail("IncompatibleFragmentException expected");
        } catch (final IncompatibleFragmentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testTraceFragmentLineAdded() throws Exception {
        final FileInRevision f1 = file("a.java", 1);
        final FileInRevision f2 = file("a.java", 2);
        final FileInRevision f3 = file("a.java", 3);

        final FileDiff diff = new FileDiff(f1).merge(new Hunk(
                new Fragment(f2, pos(5, 1), pos(5, 1)),
                new Fragment(f3, pos(5, 1), pos(6, 1))));

        final Fragment source1 = new Fragment(f1, pos(1, 1), pos(2, 1));
        final Fragment actual1 = diff.traceFragment(source1);
        assertEquals(
                new Fragment(f3, pos(1, 1), pos(2, 1),
                        source1),
                actual1);

        final Fragment source2 = new Fragment(f1, pos(7, 1), pos(9, 1));
        final Fragment actual2 = diff.traceFragment(source2);
        assertEquals(
                new Fragment(f3, pos(8, 1), pos(10, 1), source2),
                actual2);

        final Fragment source3 = new Fragment(f1, pos(2, 1), pos(7, 1));
        final Fragment actual3 = diff.traceFragment(source3);
        assertEquals(
                new Fragment(f3, pos(2, 1), pos(8, 1),
                        source3,
                        new Fragment(f3, pos(5, 1), pos(6, 1))),
                actual3);
    }

    @Test
    public void testTraceFragmentChangeInLine() throws Exception {
        final FileInRevision f1 = file("a.java", 1);
        final FileInRevision f2 = file("a.java", 2);
        final FileInRevision f3 = file("a.java", 3);

        final FileDiff diff = new FileDiff(f1).merge(new Hunk(
                new Fragment(f2, pos(5, 10), pos(5, 14)),
                new Fragment(f3, pos(5, 10), pos(5, 14))));

        final Fragment actual1 = diff.traceFragment(
                new Fragment(f1, pos(1, 1), pos(2, 1)));
        assertEquals(
                new Fragment(f3, pos(1, 1), pos(2, 1),
                        new Fragment(f1, pos(1, 1), pos(2, 1))),
                actual1);

        final Fragment actual2 = diff.traceFragment(
                new Fragment(f1, pos(7, 1), pos(9, 1)));
        assertEquals(
                new Fragment(f3, pos(7, 1), pos(9, 1),
                        new Fragment(f1, pos(7, 1), pos(9, 1))),
                actual2);

        final Fragment actual3 = diff.traceFragment(
                new Fragment(f1, pos(2, 1), pos(7, 1)));
        assertEquals(
                new Fragment(f3, pos(2, 1), pos(7, 1),
                        new Fragment(f1, pos(2, 1), pos(7, 1)),
                        new Fragment(f3, pos(5, 10), pos(5, 14))),
                actual3);

        final Fragment actual4 = diff.traceFragment(
                new Fragment(f1, pos(5, 9), pos(5, 11)));
        //TODO the fragment is enlarged, which is more or less OK, but not always necessary
        assertEquals(
                new Fragment(f3, pos(5, 9), pos(5, 14),
                        new Fragment(f1, pos(5, 9), pos(5, 11)),
                        new Fragment(f3, pos(5, 10), pos(5, 14))),
                actual4);
    }

    @Test
    public void testTraceFragmentTwoAdditionsInLine() throws Exception {
        final FileInRevision f1 = file("a.java", 1);
        final FileInRevision f2 = file("a.java", 2);
        final FileInRevision f3 = file("a.java", 3);
        final FileInRevision f4 = file("a.java", 4);

        final FileDiff diff = new FileDiff(f1)
                .merge(new Hunk(
                    new Fragment(f2, pos(5, 7), pos(5, 7)),
                    new Fragment(f3, pos(5, 7), pos(5, 9))))
                .merge(new Hunk(
                    new Fragment(f3, pos(5, 9), pos(5, 9)),
                    new Fragment(f4, pos(5, 9), pos(5, 11))));

        final Fragment actual1 = diff.traceFragment(
                new Fragment(f1, pos(1, 1), pos(6, 1)));
        assertEquals(
                new Fragment(f4, pos(1, 1), pos(6, 1),
                        new Fragment(f1, pos(1, 1), pos(6, 1)),
                        new Fragment(f3, pos(5, 7), pos(5, 9)),
                        new Fragment(f4, pos(5, 9), pos(5, 11))),
                actual1);
    }
}
