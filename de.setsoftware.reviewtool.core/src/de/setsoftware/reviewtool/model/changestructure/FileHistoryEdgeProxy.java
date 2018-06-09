package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Implements a proxy to a {@link FileHistoryEdge}.
 */
final class FileHistoryEdgeProxy extends ProxyableFileHistoryEdge {

    private static final long serialVersionUID = 5281364518525669034L;

    private final FileHistoryGraph graph;
    private final IRevisionedFile ancestorFile;
    private final IRevisionedFile descendantFile;
    private final Type type;
    private transient FileHistoryEdge target;

    /**
     * Creates a proxy to a {@link FileHistoryEdge}.
     *
     * @param graph The {@link FileHistoryGraph}.
     * @param ancestorFile The path and revision of the ancestor node.
     * @param descendantFile The path and revision of the descendant node.
     * @param type The type of the edge.
     */
    FileHistoryEdgeProxy(
            final FileHistoryGraph graph,
            final IRevisionedFile ancestorFile,
            final IRevisionedFile descendantFile,
            final Type type) {

        this.graph = graph;
        this.ancestorFile = ancestorFile;
        this.descendantFile = descendantFile;
        this.type = type;
    }

    @Override
    public FileHistoryGraph getGraph() {
        return this.graph;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public IFileDiff getDiff() {
        return this.getTarget().getDiff();
    }

    @Override
    public ProxyableFileHistoryNode getAncestor() {
        return this.getTarget().getAncestor();
    }

    @Override
    public ProxyableFileHistoryNode getDescendant() {
        return this.getTarget().getDescendant();
    }

    @Override
    void setType(Type type) {
        this.getTarget().setType(type);
    }

    /**
     * Returns the proxy target. The first time this method is called, it is looked up in the graph.
     */
    private synchronized FileHistoryEdge getTarget() {
        if (this.target == null) {
            final ProxyableFileHistoryNode ancestor = this.graph.getNodeFor(this.ancestorFile);
            assert ancestor != null;
            final ProxyableFileHistoryNode descendant = this.graph.getNodeFor(this.descendantFile);
            assert descendant != null;
            this.target = new FileHistoryEdge(this.graph, ancestor, descendant, this.type);
        }

        return this.target;
    }
}
