package de.setsoftware.reviewtool.viewtracking;

/**
 * Combines the view statistics for a Stop.
 */
public class ViewStatDataForStop {

    public static final ViewStatDataForStop NO_VIEWS = new ViewStatDataForStop(0.0, 0.0);

    private final double avg;
    private final double max;

    ViewStatDataForStop(double avg, double max) {
        assert avg <= max;
        this.avg = avg;
        this.max = max;
    }

    public boolean isNotViewedAtAll() {
        return this.max <= 0.0;
    }

    public double getMaxRatio() {
        return this.max;
    }

    public double getAverageRatio() {
        return this.avg;
    }

}
