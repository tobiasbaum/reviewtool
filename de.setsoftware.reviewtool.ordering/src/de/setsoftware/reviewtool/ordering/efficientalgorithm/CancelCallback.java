package de.setsoftware.reviewtool.ordering.efficientalgorithm;

/**
 * Interface that is used to allow cancellation of long-running operations.
 * TODO: With Java 8, one of the built-in interfaces could be used.
 */
public interface CancelCallback {

    /**
     * Returns true if the operation shall be canceled.
     */
    public abstract boolean isCanceled();

}
