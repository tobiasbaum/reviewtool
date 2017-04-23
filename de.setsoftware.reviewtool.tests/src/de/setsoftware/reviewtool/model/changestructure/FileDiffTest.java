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

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 0));
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

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(6, 1), pos(7, 0));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final Fragment f3 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 0));
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

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 0));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(new Fragment(file("a.java", 1), pos(1, 1), pos(4, 0)));
        assertEquals(expected2, ff.getFragments());
    }

    @Test
    public void testAddAdjacent2() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 0));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(new Fragment(file("a.java", 1), pos(1, 1), pos(4, 0)));
        assertEquals(expected2, ff.getFragments());
    }

    @Test
    public void testAddAdjacent3() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 0));
        ff.addFragment(f2);
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final Fragment f3 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 0));
        ff.addFragment(f3);
        ff.coalesce();
        final List<Fragment> expected3 = new ArrayList<>();
        expected3.add(new Fragment(file("a.java", 1), pos(1, 1), pos(5, 0)));
        assertEquals(expected3, ff.getFragments());
    }

    @Test
    public void testAddAdjacent4() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0));
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 0));
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final FragmentList ff2 = new FragmentList();
        final Fragment f3 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 0));
        ff2.addFragment(f3);
        final Fragment f4 = new Fragment(file("a.java", 1), pos(5, 1), pos(6, 0));
        ff2.addFragment(f4);
        ff2.coalesce();
        final List<Fragment> expected3 = new ArrayList<>();
        expected3.add(f3);
        expected3.add(f4);
        assertEquals(expected3, ff2.getFragments());

        ff.addFragmentList(ff2);
        ff.coalesce();
        final List<Fragment> expected4 = new ArrayList<>();
        expected4.add(new Fragment(file("a.java", 1), pos(1, 1), pos(6, 0)));
        assertEquals(expected4, ff.getFragments());
    }

    @Test
    public void testAddOverlapping1() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0));
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

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0));
        ff.addFragment(f1);
        final Fragment f2 = new Fragment(file("a.java", 1), pos(2, 1), pos(4, 0));
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

        final FileDiff diff = new FileDiff().merge(new Hunk(
                new Fragment(f2, pos(5, 1), pos(5, 0)),
                new Fragment(f3, pos(5, 1), pos(6, 0))));

        final Fragment actual1 = diff.traceFragment(new Fragment(f1, pos(1, 1), pos(2, 0)));
        //TODO shouldn't the resulting fragment have file revision 3 (and not 1)? applies to other tests, too.
        assertEquals(
                new Fragment(f1, pos(1, 1), pos(2, 0)),
                actual1);

        final Fragment actual2 = diff.traceFragment(new Fragment(f1, pos(7, 1), pos(9, 0)));
        assertEquals(
                new Fragment(f1, pos(8, 1), pos(10, 0)),
                actual2);

        final Fragment actual3 = diff.traceFragment(new Fragment(f1, pos(2, 1), pos(7, 0)));
        assertEquals(
                new Fragment(f1, pos(2, 1), pos(8, 0)),
                actual3);
    }

    @Test
    public void testTraceFragmentChangeInLine() throws Exception {
        final FileInRevision f1 = file("a.java", 1);
        final FileInRevision f2 = file("a.java", 2);
        final FileInRevision f3 = file("a.java", 3);

        final FileDiff diff = new FileDiff().merge(new Hunk(
                new Fragment(f2, pos(5, 10), pos(5, 14)),
                new Fragment(f3, pos(5, 10), pos(5, 14))));

        final Fragment actual1 = diff.traceFragment(
                new Fragment(f1, pos(1, 1), pos(2, 0)));
        assertEquals(
                new Fragment(f1, pos(1, 1), pos(2, 0)),
                actual1);

        final Fragment actual2 = diff.traceFragment(
                new Fragment(f1, pos(7, 1), pos(9, 0)));
        assertEquals(
                new Fragment(f1, pos(7, 1), pos(9, 0)),
                actual2);

        final Fragment actual3 = diff.traceFragment(
                new Fragment(f1, pos(2, 1), pos(7, 0)));
        assertEquals(
                new Fragment(f1, pos(2, 1), pos(7, 0)),
                actual3);

        final Fragment actual4 = diff.traceFragment(
                new Fragment(f1, pos(5, 9), pos(5, 11)));
        //TODO the fragment is enlarged, which is more or less OK, but not always necessary
        assertEquals(
                new Fragment(f3, pos(5, 9), pos(5, 14)),
                actual4);
    }

    @Test
    public void testTraceFragmentTwoAdditionsInLine() throws Exception {
        final FileInRevision f1 = file("a.java", 1);
        final FileInRevision f2 = file("a.java", 2);
        final FileInRevision f3 = file("a.java", 3);
        final FileInRevision f4 = file("a.java", 4);

        final FileDiff diff = new FileDiff()
                .merge(new Hunk(
                    new Fragment(f2, pos(5, 7), pos(5, 6)),
                    new Fragment(f3, pos(5, 7), pos(5, 8))))
                .merge(new Hunk(
                    new Fragment(f3, pos(5, 9), pos(5, 8)),
                    new Fragment(f4, pos(5, 9), pos(5, 10))));

        final Fragment actual1 = diff.traceFragment(
                new Fragment(f1, pos(1, 1), pos(6, 0)));
        assertEquals(
                new Fragment(f1, pos(1, 1), pos(6, 0)),
                actual1);
    }
}
