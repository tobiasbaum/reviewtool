package de.setsoftware.reviewtool.model.api;

/**
 * Superclass for all exceptions that may be thrown by a change source.
 */
public class ChangeSourceException extends Exception {

    private static final long serialVersionUID = -3542557287260481085L;

    private final IChangeSource changeSource;

    /**
     * Constructor.
     *
     * @param message The exception message.
     * @param cause The underlying exception that caused this exception to be thrown.
     */
    public ChangeSourceException(final IChangeSource changeSource, final String message) {
        super(message);
        this.changeSource = changeSource;
    }

    /**
     * Constructor.
     *
     * @param changeSource The change source where this exception comes from.
     * @param cause The underlying exception that caused this exception to be thrown.
     */
    public ChangeSourceException(final IChangeSource changeSource, final Throwable cause) {
        super(cause);
        this.changeSource = changeSource;
    }

    /**
     * Returns the change source where this exception comes from.
     */
    public IChangeSource getChangeSource() {
        return this.changeSource;
    }
}
