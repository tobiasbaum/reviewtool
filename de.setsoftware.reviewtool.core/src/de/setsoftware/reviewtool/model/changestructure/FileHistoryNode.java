package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A node in a {@link FileHistoryGraph}.
 */
public final class FileHistoryNode extends AbstractFileHistoryNode implements IMutableFileHistoryNode {

    private final FileInRevision file;
    private final Set<FileHistoryEdge> ancestors;
    private final Set<FileHistoryEdge> descendants;
    private FileHistoryNode parent;
    private final List<FileHistoryNode> children;
    private boolean isDeleted;

    /**
     * Creates a {@link FileHistoryNode}. The ancestor and parent are initially set to <code>null</code>.
     * @param file The {@link FileInRevision} to wrap.
     */
    public FileHistoryNode(final FileInRevision file, final boolean isDeleted) {
        this.file = file;
        this.ancestors = new LinkedHashSet<>();
        this.descendants = new LinkedHashSet<>();
        this.children = new ArrayList<>();
        this.isDeleted = isDeleted;
    }

    @Override
    public FileInRevision getFile() {
        return this.file;
    }

    @Override
    public boolean isRoot() {
        return this.ancestors.isEmpty();
    }

    @Override
    public Set<FileHistoryEdge> getAncestors() {
        return this.ancestors;
    }

    @Override
    public Set<FileHistoryEdge> getDescendants() {
        return this.descendants;
    }

    @Override
    public boolean isDeleted() {
        return this.isDeleted;
    }

    /**
     * Adds some nearest ancestor {@link FileHistoryNode}.
     * This operation is called internally when this node starts being a descendant
     * of some other {@link FileHistoryNode}.
     */
    private void addAncestor(final FileHistoryEdge ancestor) {
        this.ancestors.add(ancestor);
    }

    /**
     * Removes some nearest ancestor {@link FileHistoryNode}.
     * This operation is called internally when this node stops being a descendant
     * of some other {@link FileHistoryNode}.
     */
    void removeAncestor(final FileHistoryEdge ancestor) {
        this.ancestors.remove(ancestor);
    }

    /**
     * Adds a descendant {@link FileHistoryNode} of this node.
     */
    public void addDescendant(final FileHistoryNode descendant, final FileDiff diff) {
        final FileHistoryEdge edge = new FileHistoryEdge(this, descendant, diff);
        this.descendants.add(edge);
        descendant.addAncestor(edge);
    }

    /**
     * Adds a descendant {@link FileHistoryNode} of this node.
     */
    public boolean hasDescendant(final FileHistoryNode descendant) {
        for (final FileHistoryEdge descendantEdge : this.getDescendants()) {
            if (descendantEdge.getDescendant().equals(descendant)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets whether this node is deleted.
     * @param newDeleted {@code true} if this node should represent a deleted path, else {@code false}.
     */
    void setDeleted(final boolean newDeleted) {
        this.isDeleted = newDeleted;
    }

    /**
     * Returns a list of child {@link FileHistoryNode}s.
     */
    public List<FileHistoryNode> getChildren() {
        return this.children;
    }

    /**
     * Adds a child {@link FileHistoryNode}.
     */
    public void addChild(final FileHistoryNode child) {
        this.children.add(child);
        child.setParent(this);
    }

    /**
     * Returns <code>true</code> if this node has a parent {@link FileHistoryNode}.
     */
    public boolean hasParent() {
        return this.parent != null;
    }

    /**
     * Returns the parent {@link FileHistoryNode} or <code>null</code> if no parent has been set.
     */
    public FileHistoryNode getParent() {
        return this.parent;
    }

    /**
     * Sets the parent {@link FileHistoryNode}.
     * This operation is called internally when this node becomes/stops being a child of some other
     * {@link FileHistoryNode}.
     */
    private void setParent(final FileHistoryNode newParent) {
        this.parent = newParent;
    }

    /**
     * Returns <code>true</code> if this node results from a copy operation.
     */
    public boolean isCopied() {
        if (this.isRoot()) {
            return false;
        }
        for (final IFileHistoryEdge ancestor : this.ancestors) {
            if (!this.getFile().getPath().equals(ancestor.getAncestor().getFile().getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fills a list of additional attributes used by toString().
     * @param attributes A list containing elements to be included in the output of {@link #toString()}.
     */
    @Override
    protected void attributesToString(final List<String> attributes) {
        super.attributesToString(attributes);
        if (!this.children.isEmpty()) {
            attributes.add("children=" + this.children);
        }
    }
}
