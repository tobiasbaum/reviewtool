package de.setsoftware.reviewtool.base;

/**
 * Exception that occurred in some part of the review tool.
 */
public class ReviewtoolException extends RuntimeException {

    private static final long serialVersionUID = -5502271441844802828L;

    public ReviewtoolException(Exception nested) {
        super(nested);
    }

    public ReviewtoolException(String message, Exception nested) {
        super(message, nested);
    }

    public ReviewtoolException(String message) {
        super(message);
    }

}
