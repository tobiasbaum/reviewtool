package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryEdge;

/**
 * TODO.
 */
abstract class ProxyableFileHistoryEdge extends AbstractFileHistoryEdge implements IMutableFileHistoryEdge {

    private static final long serialVersionUID = 5891549292359250921L;

    @Override
    public abstract FileHistoryGraph getGraph();

    @Override
    public abstract ProxyableFileHistoryNode getAncestor();

    @Override
    public abstract ProxyableFileHistoryNode getDescendant();

    /**
     * Changes the type of the edge. This is used to replace a {@link Type#COPY} edge by a {@link Type#COPY_DELETED}
     * edge when a flow is terminated in a copy target.
     * @param type The new type.
     */
    abstract void setType(final Type type);
}
