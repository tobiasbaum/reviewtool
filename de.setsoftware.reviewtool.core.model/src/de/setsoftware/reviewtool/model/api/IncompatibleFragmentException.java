package de.setsoftware.reviewtool.model.api;

/**
 * Thrown if working with fragments produces an erroneous situation.
 */
public class IncompatibleFragmentException extends Exception {

    private static final long serialVersionUID = 1263298551193136251L;

    /**
     * Default constructor.
     */
    public IncompatibleFragmentException() {
    }

    /**
     * Constructor taking a message.
     * @param message The message.
     */
    public IncompatibleFragmentException(final String message) {
        super(message);
    }

    /**
     * Constructor taking a cause.
     * @param cause The cause.
     */
    public IncompatibleFragmentException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructor taking a message and a cause.
     * @param message The message.
     * @param cause The cause.
     */
    public IncompatibleFragmentException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
