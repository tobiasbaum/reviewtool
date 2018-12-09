package de.setsoftware.reviewtool.ordering.efficientalgorithm;

/**
 * Marker exception to ease some parts of the implementation.
 */
public class ReverseImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 2415934953489293307L;

    public ReverseImpossibleException() {
    }

    @Override
    public Throwable fillInStackTrace() {
        //Stacktrace not needed, marker exception
        return this;
    }
}
