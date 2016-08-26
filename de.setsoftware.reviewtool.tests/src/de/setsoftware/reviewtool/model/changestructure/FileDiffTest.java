package de.setsoftware.reviewtool.model.changestructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

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

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0), "A\nB\n");
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 0), "C\n");
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

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0), "A\nB\n");
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(6, 1), pos(7, 0), "C\n");
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final Fragment f3 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 0), "X\n");
        ff.addFragment(f3);
        ff.coalesce();
        List<Fragment> expected3 = new ArrayList<>();
        expected3.add(f1);
        expected3.add(f3);
        expected3.add(f2);
        assertEquals(expected3, ff.getFragments());
    }

    @Test
    public void testAddAdjacent1() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0), "A\nB\n");
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 0), "C\n");
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(new Fragment(file("a.java", 1), pos(1, 1), pos(4, 0), "A\nB\nC\n"));
        assertEquals(expected2, ff.getFragments());
    }

    @Test
    public void testAddAdjacent2() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 0), "C\n");
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0), "A\nB\n");
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(new Fragment(file("a.java", 1), pos(1, 1), pos(4, 0), "A\nB\nC\n"));
        assertEquals(expected2, ff.getFragments());
    }

    @Test
    public void testAddAdjacent3() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0), "A\nB\n");
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 0), "C\n");
        ff.addFragment(f2);
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final Fragment f3 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 0), "X\n");
        ff.addFragment(f3);
        ff.coalesce();
        final List<Fragment> expected3 = new ArrayList<>();
        expected3.add(new Fragment(file("a.java", 1), pos(1, 1), pos(5, 0), "A\nB\nX\nC\n"));
        assertEquals(expected3, ff.getFragments());
    }

    @Test
    public void testAddAdjacent4() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0), "A\nB\n");
        ff.addFragment(f1);
        final List<Fragment> expected1 = new ArrayList<>();
        expected1.add(f1);
        assertEquals(expected1, ff.getFragments());

        final Fragment f2 = new Fragment(file("a.java", 1), pos(4, 1), pos(5, 0), "C\n");
        ff.addFragment(f2);
        ff.coalesce();
        final List<Fragment> expected2 = new ArrayList<>();
        expected2.add(f1);
        expected2.add(f2);
        assertEquals(expected2, ff.getFragments());

        final FragmentList ff2 = new FragmentList();
        final Fragment f3 = new Fragment(file("a.java", 1), pos(3, 1), pos(4, 0), "X\n");
        ff2.addFragment(f3);
        final Fragment f4 = new Fragment(file("a.java", 1), pos(5, 1), pos(6, 0), "Y\n");
        ff2.addFragment(f4);
        ff2.coalesce();
        final List<Fragment> expected3 = new ArrayList<>();
        expected3.add(f3);
        expected3.add(f4);
        assertEquals(expected3, ff2.getFragments());

        ff.addFragmentList(ff2);
        ff.coalesce();
        final List<Fragment> expected4 = new ArrayList<>();
        expected4.add(new Fragment(file("a.java", 1), pos(1, 1), pos(6, 0), "A\nB\nX\nC\nY\n"));
        assertEquals(expected4, ff.getFragments());
    }

    @Test
    public void testAddOverlapping1() throws Exception {
        final FragmentList ff = new FragmentList();

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0), "A\nB\n");
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

        final Fragment f1 = new Fragment(file("a.java", 1), pos(1, 1), pos(3, 0), "A\nB\n");
        ff.addFragment(f1);
        final Fragment f2 = new Fragment(file("a.java", 1), pos(2, 1), pos(4, 0), "X\nY\n");
        try {
            ff.addFragment(f2);
            fail("IncompatibleFragmentException expected");
        } catch (final IncompatibleFragmentException e) {
            assertTrue(true);
        }
    }
}
