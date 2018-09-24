package de.setsoftware.reviewtool.model.changestructure;

import java.util.Set;

import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryNode;

/**
 * Implementation of a {@link IMutableFileHistoryNode} that can act as a proxy.
 */
public abstract class ProxyableFileHistoryNode extends AbstractFileHistoryNode implements IMutableFileHistoryNode {

    private static final long serialVersionUID = 5772651677502483400L;

    @Override
    public abstract Set<? extends ProxyableFileHistoryEdge> getAncestors();

    @Override
    public abstract Set<? extends ProxyableFileHistoryEdge> getDescendants();

    /**
     * Adds some nearest ancestor {@link ProxyableFileHistoryNode}.
     * This operation is called internally when this node starts being a descendant
     * of some other {@link ProxyableFileHistoryNode}.
     */
    abstract void addAncestor(final ProxyableFileHistoryEdge ancestor);

    /**
     * Removes some nearest ancestor {@link ProxyableFileHistoryNode}.
     * This operation is called internally when this node stops being a descendant
     * of some other {@link ProxyableFileHistoryNode}.
     */
    abstract void removeAncestor(final ProxyableFileHistoryEdge ancestor);

    /**
     * Adds a descendant {@link ProxyableFileHistoryNode} of this node.
     */
    abstract void addDescendant(final ProxyableFileHistoryNode descendant, final IFileHistoryEdge.Type type);

    /**
     * Makes this node a deleted node. Requires that the node is a {@link Type#ADDED} or {@link Type#CHANGED} node.
     */
    abstract void makeDeleted();

    /**
     * Makes this node a replaced node. Requires that the node is a {@link Type#DELETED} node.
     */
    abstract void makeReplaced();

    /**
     * Changes the type of this node to {@link Type#CHANGED}.
     * Requires that the node is a {@link Type#UNCONFIRMED} node.
     * In addition, {@link #makeConfirmed()} is invoked on the parent node if that exists and if it is of type
     * {@link Type#UNCONFIRMED}.
     */
    abstract void makeConfirmed();
}
