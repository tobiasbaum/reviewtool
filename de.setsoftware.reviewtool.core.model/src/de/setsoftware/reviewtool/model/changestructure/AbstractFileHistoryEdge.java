package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;

/**
 * Contains behaviour common to all {@link IFileHistoryEdge} implementations.
 */
public abstract class AbstractFileHistoryEdge implements IFileHistoryEdge {

    @Override
    public final String toString() {
        return this.getAncestor().toString() + " ==(" + this.getType().toString()
                + ")=> " + this.getDescendant().toString();
    }

}
