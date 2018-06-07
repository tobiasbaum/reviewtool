package de.setsoftware.reviewtool.model.changestructure;

import java.util.LinkedHashSet;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Implements a proxy to a {@link FileHistoryNode}.
 */
final class FileHistoryNodeProxy extends ProxyableFileHistoryNode {

    private static final long serialVersionUID = -904039120792328999L;

    private final FileHistoryGraph graph;
    private final IRevisionedFile file;
    private final Set<ProxyableFileHistoryEdge> ancestors;
    private final Set<ProxyableFileHistoryEdge> descendants;
    private final IRevisionedFile parentFile;
    private final Set<IRevisionedFile> childFiles;
    private final Type type;
    private transient FileHistoryNode target;

    /**
     * Creates a proxy to a {@link FileHistoryNode}.
     *
     * @param graph The {@link FileHistoryGraph}.
     * @param file The path and revision of the node.
     * @param ancestors The ancestor edges of the node.
     * @param descendants The descendant edges of the node.
     * @param parentFile The path and revision of the parent node.
     * @param childFiles The paths and revisions of the child nodes.
     * @param type The type of the node.
     */
    FileHistoryNodeProxy(
            final FileHistoryGraph graph,
            final IRevisionedFile file,
            final Set<ProxyableFileHistoryEdge> ancestors,
            final Set<ProxyableFileHistoryEdge> descendants,
            final IRevisionedFile parentFile,
            final Set<IRevisionedFile> childFiles,
            final Type type) {

        this.graph = graph;
        this.file = file;
        this.ancestors = ancestors;
        this.descendants = descendants;
        this.parentFile = parentFile;
        this.childFiles = childFiles;
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
        return this.getTarget().isRoot();
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public boolean isConfirmed() {
        return this.getTarget().isConfirmed();
    }

    @Override
    public boolean isCopyTarget() {
        return this.getTarget().isCopyTarget();
    }

    @Override
    public Set<? extends ProxyableFileHistoryEdge> getAncestors() {
        return this.getTarget().getAncestors();
    }

    @Override
    public Set<? extends ProxyableFileHistoryEdge> getDescendants() {
        return this.getTarget().getDescendants();
    }

    @Override
    void addAncestor(final ProxyableFileHistoryEdge ancestor) {
        this.getTarget().addAncestor(ancestor);
    }

    @Override
    void removeAncestor(final ProxyableFileHistoryEdge ancestor) {
        this.getTarget().removeAncestor(ancestor);
    }

    @Override
    void addDescendant(final ProxyableFileHistoryNode descendant, final IFileHistoryEdge.Type type) {
        this.getTarget().addDescendant(descendant, type);
    }

    @Override
    void makeDeleted() {
        this.getTarget().makeDeleted();
    }

    @Override
    void makeReplaced() {
        this.getTarget().makeReplaced();
    }

    @Override
    void makeConfirmed() {
        this.getTarget().makeConfirmed();
    }

    @Override
    Set<ProxyableFileHistoryNode> getChildren() {
        return this.getTarget().getChildren();
    }

    @Override
    void addChild(ProxyableFileHistoryNode child) {
        this.getTarget().addChild(child);
    }

    @Override
    void setHasAllChildren() {
        this.getTarget().setHasAllChildren();
    }

    @Override
    boolean hasAllChildren() {
        return this.getTarget().hasAllChildren();
    }

    @Override
    ProxyableFileHistoryNode getParent() {
        return this.getTarget().getParent();
    }

    @Override
    void setParent(ProxyableFileHistoryNode newParent) {
        this.getTarget().setParent(newParent);
    }

    @Override
    String getPathRelativeToParent() {
        return this.getTarget().getPathRelativeToParent();
    }

    /**
     * Returns the proxy target. The first time this method is called, it is looked up in the graph.
     */
    private synchronized FileHistoryNode getTarget() {
        if (this.target == null) {
            final ProxyableFileHistoryNode parent;
            if (this.parentFile != null) {
                parent = this.graph.getNodeFor(this.parentFile);
                assert parent != null;
            } else {
                parent = null;
            }

            final Set<ProxyableFileHistoryNode> children = new LinkedHashSet<>();
            for (final IRevisionedFile childFile : this.childFiles) {
                final ProxyableFileHistoryNode child = this.graph.getNodeFor(childFile);
                assert child != null;
                children.add(child);
            }

            this.target = new FileHistoryNode(
                    this.graph,
                    this.file,
                    this.ancestors,
                    this.descendants,
                    parent,
                    children,
                    this.type);
        }

        return this.target;
    }
}
