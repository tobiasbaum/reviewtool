package de.setsoftware.reviewtool.model.changestructure;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * A node in a {@link FileHistoryGraph}. It denotes a path pointing to either a directory or a file.
 * <p/>
 * A node can be in one of three states: "normal", deleted, or replaced (which basically means that
 * the chain of history has been broken deliberately).
 */
public final class FileHistoryNode extends ProxyableFileHistoryNode {

    private static final long serialVersionUID = 7938054839176372206L;

    private final FileHistoryGraph graph;
    private final IRevisionedFile file;
    private final Set<ProxyableFileHistoryEdge> ancestors;
    private final Set<ProxyableFileHistoryEdge> descendants;
    private transient ProxyableFileHistoryNode parent;
    private final Set<ProxyableFileHistoryNode> children;
    private Type type;

    /**
     * Creates a {@link FileHistoryNode}. The ancestor and parent are initially set to <code>null</code>.
     *
     * @param file The {@link IRevisionedFile} to wrap.
     * @param type The node type.
     */
    FileHistoryNode(final FileHistoryGraph graph, final IRevisionedFile file, final Type type) {
        this.graph = graph;
        this.file = file;
        this.ancestors = new LinkedHashSet<>();
        this.descendants = new LinkedHashSet<>();
        this.children = new LinkedHashSet<>();
        this.type = type;
    }

    /**
     * Creates a {@link FileHistoryNode} which is fully specified. Used by deserialization code.
     *
     * @param file The {@link IRevisionedFile} to wrap.
     * @param type The node type.
     */
    FileHistoryNode(
            final FileHistoryGraph graph,
            final IRevisionedFile file,
            final Set<ProxyableFileHistoryEdge> ancestors,
            final Set<ProxyableFileHistoryEdge> descendants,
            final ProxyableFileHistoryNode parent,
            final Set<ProxyableFileHistoryNode> children,
            final Type type) {

        this.graph = graph;
        this.file = file;
        this.ancestors = ancestors;
        this.descendants = descendants;
        this.parent = parent;
        this.children = children;
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
    public Set<ProxyableFileHistoryEdge> getAncestors() {
        return this.ancestors;
    }

    @Override
    public Set<ProxyableFileHistoryEdge> getDescendants() {
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
        for (final ProxyableFileHistoryEdge ancestorEdge : this.ancestors) {
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
        return this.file.hashCode() ^ this.type.hashCode();
    }

    @Override
    void addAncestor(final ProxyableFileHistoryEdge ancestor) {
        this.ancestors.add(ancestor);
    }

    @Override
    void removeAncestor(final ProxyableFileHistoryEdge ancestor) {
        this.ancestors.remove(ancestor);
    }

    @Override
    void addDescendant(final ProxyableFileHistoryNode descendant, final IFileHistoryEdge.Type type) {
        final ProxyableFileHistoryEdge edge = new FileHistoryEdge(this.graph, this, descendant, type);
        this.descendants.add(edge);
        descendant.addAncestor(edge);
    }

    @Override
    void makeDeleted() {
        assert this.type.equals(Type.ADDED) || this.type.equals(Type.CHANGED);
        this.type = Type.DELETED;

        final Iterator<ProxyableFileHistoryEdge> it = this.ancestors.iterator();
        while (it.hasNext()) {
            final ProxyableFileHistoryEdge ancestorEdge = it.next();
            if (ancestorEdge.getType().equals(IFileHistoryEdge.Type.COPY)) {
                ancestorEdge.setType(IFileHistoryEdge.Type.COPY_DELETED);
            }
        }
    }

    @Override
    void makeReplaced() {
        assert this.type.equals(Type.DELETED);
        this.type = Type.REPLACED;
    }

    @Override
    void makeConfirmed() {
        assert this.type.equals(Type.UNCONFIRMED);
        this.type = Type.CHANGED;

        if (this.parent != null && !this.parent.isConfirmed()) {
            this.parent.makeConfirmed();
        }
    }

    @Override
    Set<ProxyableFileHistoryNode> getChildren() {
        return this.children;
    }

    @Override
    void addChild(final ProxyableFileHistoryNode child) {
        this.children.add(child);
        child.setParent(this);
    }

    @Override
    ProxyableFileHistoryNode getParent() {
        return this.parent;
    }

    @Override
    void setParent(final ProxyableFileHistoryNode newParent) {
        this.parent = newParent;
    }

    @Override
    String getPathRelativeToParent() {
        if (this.parent == null) {
            return this.file.getPath();
        } else {
            return this.file.getPath().substring(this.parent.getFile().getPath().length());
        }
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

    /**
     * Replaces this object by a proxy when serializing.
     */
    protected final Object writeReplace() {
        return new FileHistoryNodeProxy(
                this.graph,
                this.file,
                this.ancestors,
                this.descendants,
                this.parent == null ? null : this.parent.getFile(),
                toFiles(this.children),
                this.type);
    }

    private static Set<IRevisionedFile> toFiles(final Set<ProxyableFileHistoryNode> nodes) {
        final Set<IRevisionedFile> result = new LinkedHashSet<>();
        for (final ProxyableFileHistoryNode node : nodes) {
            result.add(node.getFile());
        }
        return result;
    }
}
