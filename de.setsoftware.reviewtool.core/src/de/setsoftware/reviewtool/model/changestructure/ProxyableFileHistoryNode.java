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

    /**
     * Returns a list of child {@link ProxyableFileHistoryNode}s.
     */
    abstract Set<ProxyableFileHistoryNode> getChildren();

    /**
     * Adds a child {@link ProxyableFileHistoryNode}.
     */
    abstract void addChild(final ProxyableFileHistoryNode child);

    /**
     * Returns {@code true} iff all children of this node are known to exist (as far as the history is known).
     */
    abstract boolean hasAllChildren();

    /**
     * Makes this node aware of the fact that all of its children (as far as the history is known) are known to exist.
     * This is used as an optimization in order to prevent unnecessary graph traversals.
     */
    abstract void setHasAllChildren();

    /**
     * Returns the parent {@link ProxyableFileHistoryNode} or <code>null</code> if no parent has been set.
     */
    abstract ProxyableFileHistoryNode getParent();

    /**
     * Sets the parent {@link ProxyableFileHistoryNode}.
     * This operation is called internally when this node becomes/stops being a child of some other
     * {@link ProxyableFileHistoryNode}.
     */
    abstract void setParent(final ProxyableFileHistoryNode newParent);

    /**
     * Returns the path of this node relative to its parent.
     * If this node does not have a parent, the node's path is returned unmodified.
     */
    abstract String getPathRelativeToParent();
}
