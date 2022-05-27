package de.setsoftware.reviewtool.viewtracking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.setsoftware.reviewtool.model.viewtracking.ViewStatisticsForFile;

/**
 * Tests for {@link ViewStatisticsForFile}.
 */
public class ViewStatisticsForFileTest {

    private static double DELTA = 0.000000001;

    @Test
    public void testRatioIsZeroWhenNothingMarked() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        assertEquals(0.0, s.determineViewRatio(1, 100, 5).getAverageRatio(), DELTA);
        assertEquals(0.0, s.determineViewRatioWithoutPosition(5).getAverageRatio(), DELTA);
        assertEquals(0.0, s.determineViewRatio(1, 100, 5).getMaxRatio(), DELTA);
        assertEquals(0.0, s.determineViewRatioWithoutPosition(5).getMaxRatio(), DELTA);
    }

    @Test
    public void testRatioOnlyWholeFileHalfViewed() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        assertEquals(0.5, s.determineViewRatioWithoutPosition(6).getAverageRatio(), DELTA);
        assertEquals(0.5, s.determineViewRatioWithoutPosition(6).getMaxRatio(), DELTA);
    }

    @Test
    public void testRatioOnlyWholeFileFullViewed() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        assertEquals(1.0, s.determineViewRatioWithoutPosition(6).getAverageRatio(), DELTA);
    }

    @Test
    public void testRatioOnlyWholeFileMoreThanFullViewed() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        assertEquals(1.0, s.determineViewRatioWithoutPosition(6).getAverageRatio(), DELTA);
    }

    @Test
    public void testRatioFallbackToWholeFileHalfViewed() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        s.markUnknownPosition();
        s.markUnknownPosition();
        assertEquals(0.5, s.determineViewRatio(1, 100, 4).getAverageRatio(), DELTA);
        assertEquals(0.5, s.determineViewRatio(1, 100, 4).getMaxRatio(), DELTA);
    }

    @Test
    public void testRatioFallbackToWholeFileFullViewed() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        assertEquals(1.0, s.determineViewRatio(1, 100, 4).getAverageRatio(), DELTA);
    }

    @Test
    public void testRatioFallbackToWholeFileMoreThanFullViewed() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        s.markUnknownPosition();
        assertEquals(1.0, s.determineViewRatio(1, 100, 4).getAverageRatio(), DELTA);
    }

    @Test
    public void testViewRatioForSingleLines() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        s.mark(1, 5);
        s.mark(2, 5);
        s.mark(3, 5);
        s.mark(4, 5);
        s.mark(5, 5);
        assertEquals(0.2, s.determineViewRatio(1, 1, 5).getAverageRatio(), DELTA);
        assertEquals(0.4, s.determineViewRatio(2, 2, 5).getAverageRatio(), DELTA);
        assertEquals(0.6, s.determineViewRatio(3, 3, 5).getAverageRatio(), DELTA);
        assertEquals(0.8, s.determineViewRatio(4, 4, 5).getAverageRatio(), DELTA);
        assertEquals(1.0, s.determineViewRatio(5, 5, 5).getAverageRatio(), DELTA);
        assertEquals(0.0, s.determineViewRatio(6, 6, 5).getAverageRatio(), DELTA);
    }

    @Test
    public void testForMultipleLinesAverageIsUsed() {
        final ViewStatisticsForFile s = new ViewStatisticsForFile();
        s.mark(31, 40);
        s.mark(31, 40);
        s.mark(36, 40);
        s.mark(36, 40);
        assertEquals(0.375, s.determineViewRatio(31, 40, 8).getAverageRatio(), DELTA);
        assertEquals(0.375, s.determineViewRatio(32, 39, 8).getAverageRatio(), DELTA);
        assertEquals(0.5, s.determineViewRatio(31, 40, 8).getMaxRatio(), DELTA);
        assertEquals(0.5, s.determineViewRatio(32, 39, 8).getMaxRatio(), DELTA);
        assertFalse(s.determineViewRatio(31, 40, 8).isPartlyUnvisited());
        assertFalse(s.determineViewRatio(31, 32, 8).isPartlyUnvisited());
        assertTrue(s.determineViewRatio(30, 40, 8).isPartlyUnvisited());
    }

}
