package de.setsoftware.reviewtool.model.remarks;

/**
 * An exception that occurred during review remark processing.
 */
public class ReviewRemarkException extends RuntimeException {

    private static final long serialVersionUID = -3407790654510912791L;

    public ReviewRemarkException(Throwable e) {
        super(e);
    }

    public ReviewRemarkException(String message) {
        super(message);
    }

}
