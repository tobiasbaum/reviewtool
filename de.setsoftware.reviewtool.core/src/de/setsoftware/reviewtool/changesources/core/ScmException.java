package de.setsoftware.reviewtool.changesources.core;

/**
 * Superclass for all exceptions that may be thrown by the Source Code Management (SCM) layer.
 */
public class ScmException extends Exception {

    private static final long serialVersionUID = 5577100524043603209L;

    /**
     * Constructor.
     * @param message The exception message.
     */
    public ScmException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param message The exception message.
     * @param cause The underlying exception that caused this exception to be thrown.
     */
    public ScmException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     * @param cause The underlying exception that caused this exception to be thrown.
     */
    public ScmException(final Throwable cause) {
        super(cause);
    }
}
