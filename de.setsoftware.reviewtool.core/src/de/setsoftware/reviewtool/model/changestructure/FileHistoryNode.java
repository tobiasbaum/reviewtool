package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * A node in a {@link FileHistoryGraph}. It denotes a path pointing to either a directory or a file.
 * <p/>
 * A node can be in one of three states: "normal", deleted, or replaced (which basically means that
 * the chain of history has been broken deliberately).
 */
public final class FileHistoryNode extends AbstractFileHistoryNode implements IMutableFileHistoryNode {

    private final FileHistoryGraph graph;
    private final IRevisionedFile file;
    private final Set<FileHistoryEdge> ancestors;
    private final Set<FileHistoryEdge> descendants;
    private FileHistoryNode parent;
    private final List<FileHistoryNode> children;
    private Type type;

    /**
     * Creates a {@link FileHistoryNode}. The ancestor and parent are initially set to <code>null</code>.
     *
     * @param file The {@link IRevisionedFile} to wrap.
     */
    public FileHistoryNode(final FileHistoryGraph graph, final IRevisionedFile file, final Type type) {
        this.graph = graph;
        this.file = file;
        this.ancestors = new LinkedHashSet<>();
        this.descendants = new LinkedHashSet<>();
        this.children = new ArrayList<>();
        this.type = type;
    }

    @Override
    public FileHistoryGraph getGraph() {
        return this.graph;
    }

    @Override
    public IRevisionedFile getFile() {
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
    public Type getType() {
        return this.type;
    }

    @Override
    public boolean isConfirmed() {
        return !this.type.equals(Type.UNCONFIRMED);
    }

    @Override
    public boolean isCopyTarget() {
        for (final FileHistoryEdge ancestorEdge : this.ancestors) {
            if (ancestorEdge.getType().equals(IFileHistoryEdge.Type.COPY)) {
                return true;
            } else if (this.type.equals(Type.DELETED)
                    && ancestorEdge.getType().equals(IFileHistoryEdge.Type.COPY_DELETED)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Only internal properties of the nodes are compared, no properties that would require traversing the graph.
     * So neither ancestors nor descendants, neither parent nodes nor child nodes are compared.
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof FileHistoryNode) {
            final FileHistoryNode other = (FileHistoryNode) o;
            return this.file.equals(other.file)
                    && this.type.equals(other.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.file.hashCode();
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
    void addDescendant(final FileHistoryNode descendant, final IFileHistoryEdge.Type type, final IFileDiff diff) {
        final FileHistoryEdge edge = new FileHistoryEdge(this.graph, this, descendant, type, diff);
        this.descendants.add(edge);
        descendant.addAncestor(edge);
    }

    /**
     * Makes this node a deleted node. Requires that the node is a {@link Type#NORMAL} node.
     */
    void makeDeleted() {
        assert this.type.equals(Type.NORMAL);
        this.type = Type.DELETED;

        final Iterator<FileHistoryEdge> it = this.ancestors.iterator();
        while (it.hasNext()) {
            final FileHistoryEdge ancestorEdge = it.next();
            if (ancestorEdge.getType().equals(IFileHistoryEdge.Type.COPY)) {
                ancestorEdge.setType(IFileHistoryEdge.Type.COPY_DELETED);
            }
        }
    }

    /**
     * Makes this node a replaced node. Requires that the node is a {@link Type#DELETED} node.
     */
    void makeReplaced() {
        assert this.type.equals(Type.DELETED);
        this.type = Type.REPLACED;
    }

    /**
     * Makes this node a confirmed node. Requires that the node is a {@link Type#UNCONFIRMED} node.
     */
    void makeConfirmed() {
        assert this.type.equals(Type.UNCONFIRMED);
        this.type = Type.NORMAL;
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
