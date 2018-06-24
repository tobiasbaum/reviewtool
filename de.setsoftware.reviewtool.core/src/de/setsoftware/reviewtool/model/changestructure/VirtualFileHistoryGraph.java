package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.PartialOrderAlgorithms;
import de.setsoftware.reviewtool.model.api.IDiffAlgorithm;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Merges two file history graphs (typically a remote and a local one) into one combined file history graph.
 * Note that the nodes returned are snapshots and do not change if the underlying file history graphs change afterwards.
 */
public final class VirtualFileHistoryGraph extends AbstractFileHistoryGraph {

    private final IFileHistoryGraph remoteFileHistoryGraph;
    private IFileHistoryGraph localFileHistoryGraph;
    private final Map<IRevisionedFile, IFileHistoryNode> virtualNodes;

    public VirtualFileHistoryGraph(final IFileHistoryGraph remoteFileHistoryGraph) {
        this.remoteFileHistoryGraph = remoteFileHistoryGraph;
        this.localFileHistoryGraph = null;
        this.virtualNodes = new LinkedHashMap<>();
    }

    /**
     * Returns the underlying local file history graph.
     * @return The {@link IFileHistoryGraph local file history graph}. May be {@code null}.
     */
    public IFileHistoryGraph getLocalFileHistoryGraph() {
        return this.localFileHistoryGraph;
    }

    /**
     * Sets or unsets the local file history graph.
     * @param localFileHistoryGraph The new local file history graph. May be {@code null}.
     */
    public void setLocalFileHistoryGraph(final IFileHistoryGraph localFileHistoryGraph) {
        this.localFileHistoryGraph = localFileHistoryGraph;
        this.virtualNodes.clear();
        if (this.localFileHistoryGraph != null) {
            this.computeIntermediateNodes();
        }
    }

    /**
     * Returns the underlying remote file history graph.
     * @return The {@link IFileHistoryGraph remote file history graph}.
     */
    public IFileHistoryGraph getRemoteFileHistoryGraph() {
        return this.remoteFileHistoryGraph;
    }

    private void computeIntermediateNodes() {
        for (final IFileHistoryNode localNode : this.localFileHistoryGraph.getIncompleteFlowStarts()) {
            this.computeIntermediateNodes(localNode);
        }
    }

    private void computeIntermediateNodes(final IFileHistoryNode localNode) {
        IFileHistoryNode remoteNode = this.remoteFileHistoryGraph.getNodeFor(localNode.getFile());
        if (remoteNode != null) {
            final VirtualFileHistoryNode virtualNode = new VirtualFileHistoryNode(
                    this,
                    localNode.getFile(),
                    Arrays.asList(remoteNode, localNode));
            this.virtualNodes.put(virtualNode.getFile(), virtualNode);
            return;
        }

        remoteNode = this.remoteFileHistoryGraph.findAncestorFor(localNode.getFile());
        if (remoteNode != null) {
            final VirtualFileHistoryNode virtualAncestorNode = new VirtualFileHistoryNode(
                    this,
                    remoteNode.getFile(),
                    Collections.singletonList(remoteNode));
            final VirtualFileHistoryNode virtualDescendantNode = new VirtualFileHistoryNode(
                    this,
                    localNode.getFile(),
                    Collections.singletonList(localNode));

            virtualAncestorNode.addDescendant(virtualDescendantNode);
            virtualDescendantNode.addAncestor(virtualAncestorNode);

            this.virtualNodes.put(virtualAncestorNode.getFile(), virtualAncestorNode);
            this.virtualNodes.put(virtualDescendantNode.getFile(), virtualDescendantNode);
        }
    }

    @Override
    public Set<String> getPaths() {
        final Set<String> result = new LinkedHashSet<>();
        result.addAll(this.remoteFileHistoryGraph.getPaths());
        result.addAll(this.localFileHistoryGraph.getPaths());
        return result;
    }

    @Override
    public IFileHistoryNode getNodeFor(final IRevisionedFile file) {
        final IFileHistoryNode virtualNode = this.virtualNodes.get(file);
        if (virtualNode != null) {
            return virtualNode;
        }

        final List<IFileHistoryNode> nodes = new ArrayList<>();

        final IFileHistoryNode remoteNode = this.remoteFileHistoryGraph.getNodeFor(file);
        if (remoteNode != null) {
            nodes.add(remoteNode);
        }

        final IFileHistoryNode localNode = this.localFileHistoryGraph != null
                ? this.localFileHistoryGraph.getNodeFor(file)
                : null;
        if (localNode != null) {
            nodes.add(localNode);
        }

        if (!nodes.isEmpty()) {
            final VirtualFileHistoryNode node = new VirtualFileHistoryNode(this, file, nodes);
            return node;
        } else {
            return null;
        }
    }

    @Override
    public IFileHistoryNode findAncestorFor(final IRevisionedFile file) {
        final Multimap<IRevisionedFile, IFileHistoryNode> nodeMap = new Multimap<>();

        final IFileHistoryNode remoteAncestor = this.remoteFileHistoryGraph.findAncestorFor(file);
        if (remoteAncestor != null) {
            nodeMap.put(remoteAncestor.getFile(), remoteAncestor);
        }

        final IFileHistoryNode localAncestor = this.localFileHistoryGraph.findAncestorFor(file);
        if (localAncestor != null) {
            nodeMap.put(localAncestor.getFile(), localAncestor);
        }

        if (!nodeMap.isEmpty()) {
            final List<IRevisionedFile> sortedFiles = PartialOrderAlgorithms.topoSort(nodeMap.keySet());
            final List<IFileHistoryNode> nodes = nodeMap.get(sortedFiles.get(sortedFiles.size() - 1));
            return new VirtualFileHistoryNode(this, file, nodes);
        } else {
            return null;
        }
    }

    @Override
    public final Set<IFileHistoryNode> getIncompleteFlowStarts() {
        final Set<IFileHistoryNode> result = new LinkedHashSet<>();
        result.addAll(this.remoteFileHistoryGraph.getIncompleteFlowStarts());
        result.addAll(this.localFileHistoryGraph.getIncompleteFlowStarts());
        return result;
    }

    /**
     * Returns the difference algorithm of the remote file history graph.
     */
    @Override
    public IDiffAlgorithm getDiffAlgorithm() {
        return this.remoteFileHistoryGraph.getDiffAlgorithm();
    }
}
