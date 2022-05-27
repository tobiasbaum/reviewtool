package de.setsoftware.reviewtool.model.viewtracking;

/**
 * Combines the view statistics for a Stop.
 */
public class ViewStatDataForStop {

    public static final ViewStatDataForStop NO_VIEWS = new ViewStatDataForStop(0.0, 0.0, 0.0);

    private final double avg;
    private final double max;
    private final double min;

    ViewStatDataForStop(double avg, double max, double min) {
        assert avg <= max : "avg=" + avg + ", max=" + max;
        assert min <= avg : "avg=" + avg + ", min=" + min;
        this.avg = avg;
        this.max = max;
        this.min = min;
    }

    public boolean isNotViewedAtAll() {
        return this.max <= 0.0;
    }

    public boolean isPartlyUnvisited() {
        return this.min <= 0.0;
    }

    public double getMaxRatio() {
        return this.max;
    }

    public double getAverageRatio() {
        return this.avg;
    }

}
