package de.setsoftware.reviewtool.viewtracking;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.StubRepo;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.viewtracking.ViewStatistics.INextStopCallback;

public class ViewStatisticsTest {

    private static final class CallbackStub implements INextStopCallback {
        private Tour tour;
        private boolean wrapped;

        @Override
        public void newTourStarted(Tour tour) {
            assertEquals(null, this.tour);
            this.tour = tour;
        }

        @Override
        public void wrappedAround() {
            this.wrapped = true;
        }
    }

    private static Stop stop(String string) {
        final FileInRevision file = ChangestructureFactory.createFileInRevision(
                string, ChangestructureFactory.createLocalRevision(), new StubRepo());
        return new Stop(file, file, file);
    }

    private static void doTest(
            ViewStatistics stats,
            ToursInReview tours,
            Stop currentStop,
            Stop expectedNextStop,
            Tour expectedNewTour,
            boolean expectWrapAround) {

        final CallbackStub stub = new CallbackStub();
        final Stop next = stats.getNextUnvisitedStop(
                tours, currentStop, stub);
        assertEquals(expectedNextStop, next);
        assertEquals(expectedNewTour, stub.tour);
        assertEquals(expectWrapAround, stub.wrapped);
    }

    @Test
    public void testGetNextUnvisitedStopWithEmptyTour() {
        final ToursInReview tours = ToursInReview.create(Collections.<Tour>emptyList());
        doTest(new ViewStatistics(), tours, null, null, null, false);
    }

    @Test
    public void testGetNextUnvisitedStopWithNonExistingStop() {
        final ToursInReview tours = ToursInReview.create(Collections.<Tour>emptyList());
        doTest(new ViewStatistics(), tours, stop("a"), null, null, false);
    }

    @Test
    public void testGetNextUnvisitedStopWhenCurrentIsTheOnly() {
        final Stop s = stop("a");
        final Tour t = new Tour("t1", Arrays.asList(s));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t));
        doTest(new ViewStatistics(), tours, s, null, null, false);
    }

    @Test
    public void testGetNextUnvisitedStopReturnsFirstWithNullArg() {
        final Stop s = stop("a");
        final Tour t = new Tour("t1", Arrays.asList(s));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t));
        doTest(new ViewStatistics(), tours, null, s, t, false);
    }

    @Test
    public void testGetNextUnvisitedStopNormalCase() {
        final Stop s1 = stop("a");
        final Stop s2 = stop("b");
        final Stop s3 = stop("c");
        final Tour t = new Tour("t1", Arrays.asList(s1, s2, s3));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t));
        doTest(new ViewStatistics(), tours, s2, s3, null, false);
    }

    @Test
    public void testGetNextUnvisitedStopWrapAround() {
        final Stop s1 = stop("a");
        final Stop s2 = stop("b");
        final Stop s3 = stop("c");
        final Tour t = new Tour("t1", Arrays.asList(s1, s2, s3));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t));
        doTest(new ViewStatistics(), tours, s3, s1, null, true);
    }

    @Test
    public void testGetNextUnvisitedStopMoveToNextTour() {
        final Stop s1 = stop("a");
        final Stop s2 = stop("b");
        final Stop s3 = stop("c");
        final Tour t1 = new Tour("t1", Arrays.asList(s1, s2, s3));
        final Stop s4 = stop("d");
        final Stop s5 = stop("e");
        final Tour t2 = new Tour("t2", Arrays.asList(s4, s5));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t1, t2));
        doTest(new ViewStatistics(), tours, s3, s4, t2, false);
    }

    @Test
    public void testGetNextUnvisitedStopWrapAroundWithTwoTours() {
        final Stop s1 = stop("a");
        final Stop s2 = stop("b");
        final Stop s3 = stop("c");
        final Tour t1 = new Tour("t1", Arrays.asList(s1, s2, s3));
        final Stop s4 = stop("d");
        final Stop s5 = stop("e");
        final Tour t2 = new Tour("t2", Arrays.asList(s4, s5));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t1, t2));
        doTest(new ViewStatistics(), tours, s5, s1, t1, true);
    }

    @Test
    public void testGetNextUnvisitedVisitedAreNotChosen() {
        final Stop s1 = stop("a");
        final Stop s2 = stop("b");
        final Stop s3 = stop("c");
        final Tour t1 = new Tour("t1", Arrays.asList(s1, s2, s3));
        final Stop s4 = stop("d");
        final Stop s5 = stop("e");
        final Tour t2 = new Tour("t2", Arrays.asList(s4, s5));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t1, t2));

        final ViewStatistics stats = new ViewStatistics();
        stats.markUnknownPosition(s3.getAbsoluteFile());
        stats.markUnknownPosition(s4.getAbsoluteFile());

        doTest(stats, tours, s2, s5, t2, false);
    }

    @Test
    public void testGetNextUnvisitedNullWhenNoUnvisitedLeftExceptCurrent() {
        final Stop s1 = stop("a");
        final Stop s2 = stop("b");
        final Stop s3 = stop("c");
        final Tour t1 = new Tour("t1", Arrays.asList(s1, s2, s3));
        final Stop s4 = stop("d");
        final Stop s5 = stop("e");
        final Tour t2 = new Tour("t2", Arrays.asList(s4, s5));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t1, t2));

        final ViewStatistics stats = new ViewStatistics();
        stats.markUnknownPosition(s1.getAbsoluteFile());
        stats.markUnknownPosition(s3.getAbsoluteFile());
        stats.markUnknownPosition(s4.getAbsoluteFile());
        stats.markUnknownPosition(s5.getAbsoluteFile());

        doTest(stats, tours, s2, null, null, false);
    }

    @Test
    public void testSkipEmptyTours() {
        final Stop s1 = stop("a");
        final Tour t1 = new Tour("t1", Arrays.asList(s1));
        final Tour t2 = new Tour("t2", Collections.<Stop>emptyList());
        final Tour t3 = new Tour("t3", Collections.<Stop>emptyList());
        final Stop s2 = stop("b");
        final Tour t4 = new Tour("t4", Arrays.asList(s2));
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t1, t2, t3, t4));

        doTest(new ViewStatistics(), tours, s1, s2, t4, false);
    }

    @Test
    public void testSkipEmptyToursAtEnd() {
        final Stop s1 = stop("a");
        final Tour t1 = new Tour("t1", Arrays.asList(s1));
        final Tour t2 = new Tour("t2", Collections.<Stop>emptyList());
        final Tour t3 = new Tour("t3", Collections.<Stop>emptyList());
        final Tour t4 = new Tour("t4", Collections.<Stop>emptyList());
        final ToursInReview tours = ToursInReview.create(Arrays.asList(t1, t2, t3, t4));

        doTest(new ViewStatistics(), tours, s1, null, null, false);
    }
}
