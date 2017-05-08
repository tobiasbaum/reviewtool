package de.setsoftware.reviewtool.model.api;

/**
 * Visitor-Pattern: Interface that can be implemented when different behavior
 * is needed for the different subclasses of {@link IChange}.
 */
public interface IChangeVisitor {

    /**
     * Handles a textual change.
     * @param visitee The change.
     */
    public abstract void handle(ITextualChange visitee);

    /**
     * Handles a binary change.
     * @param visitee The change.
     */
    public abstract void handle(IBinaryChange visitee);

}
