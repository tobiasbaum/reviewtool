package de.setsoftware.reviewtool.ordering.efficientalgorithm;

/**
 * Interface that is used to allow interaction with the potentially long-running sorting/grouping operation.
 */
public interface TourCalculatorControl {

    /**
     * Returns true if the operation shall be canceled.
     */
    public abstract boolean isCanceled();

    /**
     * Returns true if the operation shall be accelerated (possibly leading to less optimal results).
     */
    public abstract boolean isFastModeNeeded();

}
