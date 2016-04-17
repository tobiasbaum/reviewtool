package de.setsoftware.reviewtool.model.changestructure;

/**
 * A singular change in a commit.
 */
public abstract class Change {

    /**
     * Visitor-Pattern: Calls the method in the visitor that corresponds to this
     * change's type.
     */
    public abstract void accept(ChangeVisitor visitor);

}
