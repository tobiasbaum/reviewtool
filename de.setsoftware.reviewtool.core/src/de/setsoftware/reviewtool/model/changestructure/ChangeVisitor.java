package de.setsoftware.reviewtool.model.changestructure;

/**
 * Visitor-Pattern: Interface that can be implemented when different behavior
 * is needed for the different subclasses of {@link Change}.
 */
public interface ChangeVisitor {

    public abstract void handle(TextualChange visitee);

    public abstract void handle(BinaryChange visitee);

}
