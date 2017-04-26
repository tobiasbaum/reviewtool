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
public abstract class FileHistoryNode implements IFileHistoryNode {

    private final FileInRevision file;
    private Set<FileHistoryEdge> ancestors;
    private FileHistoryNode parent;
    private final List<FileHistoryNode> children;
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
    public FileHistoryNode(final FileInRevision file) {
        this.file = file;
        this.ancestors = new LinkedHashSet<>();
        this.children = new ArrayList<>();
    }

    @Override
    public final FileInRevision getFile() {
        return this.file;
    }

    @Override
    public final boolean isRoot() {
        return this.ancestors.isEmpty();
    }

    @Override
    public final Set<? extends FileHistoryEdge> getAncestors() {
        return this.ancestors;
    }

    @Override
    public final Set<FileDiff> buildHistories(final IFileHistoryNode from) {
        if (from.equals(this)) {
            return Collections.singleton(new FileDiff(from.getFile()));
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
    protected final void addAncestor(final FileHistoryEdge ancestor) {
        this.ancestors.add(ancestor);
    }

    /**
     * Removes some nearest ancestor {@link FileHistoryNode}.
     * This operation is called internally when this node stops being a descendant
     * of some other {@link FileHistoryNode}.
     */
    protected final void removeAncestor(final FileHistoryEdge ancestor) {
        this.ancestors.remove(ancestor);
    }

    /**
     * Returns a list of child  s.
     */
    public final List<FileHistoryNode> getChildren() {
        return this.children;
    }

    /**
     * Adds a child {@link FileHistoryNode}.
     */
    public final void addChild(final FileHistoryNode child) {
        this.children.add(child);
        child.setParent(this);
    }

    /**
     * Returns <code>true</code> if this node has a parent {@link FileHistoryNode}.
     */
    public final boolean hasParent() {
        return this.parent != null;
    }

    /**
     * Returns the parent {@link FileHistoryNode} or <code>null</code> if no parent has been set.
     */
    public final FileHistoryNode getParent() {
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
    public final boolean isCopied() {
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
     * Accepts a {@link FileHistoryNodeVisitor}.
     */
    public abstract void accept(FileHistoryNodeVisitor v);

    /**
     * {@inheritDoc}
     * <p/>
     * In order to prevent infinite recursion, a thread-local flag <code>inToString</code> is used to detect cycles.
     */
    @Override
    public final String toString() {
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
    protected void attributesToString(final List<String> attributes) {
        if (!this.isRoot()) {
            final Set<IFileHistoryNode> nodes = new LinkedHashSet<>();
            for (final IFileHistoryEdge ancestorEdge : this.ancestors) {
                nodes.add(ancestorEdge.getAncestor());
            }
            attributes.add("ancestors=" + nodes);
        }
        if (!this.children.isEmpty()) {
            attributes.add("children=" + this.children);
        }
    }
}
