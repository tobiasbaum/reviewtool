package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * A node in a {@link FileHistoryGraph}.
 */
public final class FileHistoryNode implements IFileHistoryNode {

    private final FileInRevision file;
    private final Set<FileHistoryEdge> ancestors;
    private final Set<FileHistoryEdge> descendants;
    private FileHistoryNode parent;
    private final List<FileHistoryNode> children;
    private boolean isDeleted;
    private static final ThreadLocal<Boolean> inToString = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

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

    @Override
    public Set<FileDiff> buildHistories(final IFileHistoryNode from) {
        if (from.equals(this)) {
            return Collections.singleton(new FileDiff(from.getFile(), from.getFile()));
        }

        if (!this.isRoot()) {
            final Set<FileDiff> result = new LinkedHashSet<>();
            for (final IFileHistoryEdge ancestorEdge : this.ancestors) {
                for (final FileDiff diff : ancestorEdge.getAncestor().buildHistories(from)) {
                    try {
                        result.add(diff.merge(ancestorEdge.getDiff()));
                    } catch (final IncompatibleFragmentException e) {
                        throw new ReviewtoolException(e);
                    }
                }
            }
            return result;
        } else {
            return Collections.emptySet(); // ancestor revision not found
        }
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
     * {@inheritDoc}
     * <p/>
     * In order to prevent infinite recursion, a thread-local flag <code>inToString</code> is used to detect cycles.
     */
    @Override
    public String toString() {
        if (inToString.get()) {
            return this.file.toString();
        }

        inToString.set(true);
        try {
            final List<String> attributes = new ArrayList<>();
            this.attributesToString(attributes);
            if (attributes.isEmpty()) {
                return this.file.toString();
            } else {
                return this.file.toString() + attributes.toString();
            }
        } finally {
            inToString.set(false);
        }
    }

    /**
     * Fills a list of additional attributes used by toString().
     * @param attributes A list containing elements to be included in the output of {@link #toString()}.
     */
    private void attributesToString(final List<String> attributes) {
        if (!this.isRoot()) {
            final Set<IFileHistoryNode> nodes = new LinkedHashSet<>();
            for (final IFileHistoryEdge ancestorEdge : this.ancestors) {
                nodes.add(ancestorEdge.getAncestor());
            }
            attributes.add("ancestors=" + nodes);
        }
        if (!this.descendants.isEmpty()) {
            final Set<IFileHistoryNode> nodes = new LinkedHashSet<>();
            for (final IFileHistoryEdge descendantEdge : this.descendants) {
                nodes.add(descendantEdge.getDescendant());
            }
            attributes.add("descendants=" + nodes);
        }
        if (!this.children.isEmpty()) {
            attributes.add("children=" + this.children);
        }
        if (this.isDeleted) {
            attributes.add("deleted");
        }
    }
}
