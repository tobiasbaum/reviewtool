package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.PartialOrderAlgorithms;
import de.setsoftware.reviewtool.model.api.IDiffAlgorithm;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
public final class FileHistoryGraph extends AbstractFileHistoryGraph implements IMutableFileHistoryGraph {

    private static final long serialVersionUID = -1211314455688759596L;

    private final IDiffAlgorithm diffAlgorithm;
    private final Multimap<String, ProxyableFileHistoryNode> index;
    private final Multimap<IRevisionedFile, IFileHistoryNode> incompleteFlowStarts;

    /**
     * Constructor.
     * @param diffAlgorithm The algorithm to be used for computing differences between file revisions.
     */
    public FileHistoryGraph(final IDiffAlgorithm diffAlgorithm) {
        this.diffAlgorithm = diffAlgorithm;
        this.index = new Multimap<>();
        this.incompleteFlowStarts = new Multimap<>();
    }

    @Override
    public final Set<String> getPaths() {
        return this.index.keySet();
    }

    @Override
    public final void addAddition(
            final String path,
            final IRevision revision) {

        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        final ProxyableFileHistoryNode node = this.getOrCreateConnectedNode(file, IFileHistoryNode.Type.ADDED);
        if (node.getType().equals(IFileHistoryNode.Type.DELETED)) {
            node.makeReplaced();
        }
    }

    @Override
    public final void addChange(
            final String path,
            final IRevision revision,
            final Set<? extends IRevision> ancestorRevisions) {

        assert !ancestorRevisions.isEmpty();
        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        final ProxyableFileHistoryNode node = this.getOrCreateChangeNode(file);
        assert !node.getType().equals(IFileHistoryNode.Type.DELETED);

        if (node.isRoot()) {
            // for each root file within the history graph, we need an ancestor node to record the changes
            for (final IRevision ancestorRevision : ancestorRevisions) {
                final IRevisionedFile prevFile = ChangestructureFactory.createFileInRevision(path, ancestorRevision);
                final ProxyableFileHistoryNode ancestor = this.getOrCreateConnectedNode(
                        prevFile,
                        IFileHistoryNode.Type.UNCONFIRMED);
                ancestor.addDescendant(node, IFileHistoryEdge.Type.NORMAL);
            }
        }
        //else: a change is being recorded for a node copied in the same commit, so passed ancestors can be ignored
    }

    @Override
    public final void addDeletion(
            final String path,
            final IRevision revision) {

        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        final ProxyableFileHistoryNode node = this.getOrCreateConnectedNode(file, IFileHistoryNode.Type.CHANGED);
        node.makeDeleted();

        this.createMissingChildren(node);
        for (final ProxyableFileHistoryNode child : node.getChildren()) {
            final IRevisionedFile childFile = child.getFile();
            assert revision.equals(childFile.getRevision());
            this.addDeletion(childFile.getPath(), revision);
        }
    }

    @Override
    public final void addCopy(
            final String pathFrom,
            final String pathTo,
            final IRevision revisionFrom,
            final IRevision revisionTo) {

        final IRevisionedFile fileFrom = ChangestructureFactory.createFileInRevision(pathFrom, revisionFrom);
        final IRevisionedFile fileTo = ChangestructureFactory.createFileInRevision(pathTo, revisionTo);

        final ProxyableFileHistoryNode fromNode =
                this.getOrCreateConnectedNode(fileFrom, IFileHistoryNode.Type.UNCONFIRMED);
        final ProxyableFileHistoryNode toNode = this.getOrCreateUnconnectedNode(fileTo, IFileHistoryNode.Type.CHANGED);
        if (toNode.getType().equals(IFileHistoryNode.Type.DELETED)) {
            toNode.makeReplaced();
        }

        this.createMissingChildren(fromNode);

        /*
         * This can result in two copy edges (one of type COPY and one of type COPY_DELETED) between two nodes.
         * Consider the following Subversion commands:
         *
         *   mkdir -p trunk/x
         *   > trunk/x/a
         *   svn add trunk; svn ci -m "." trunk; svn update
         *   # copy trunk --> trunk2, which also copies trunk/x --> trunk2/x and trunk/x/a --> trunk2/x/a
         *   svn copy trunk trunk2
         *   # replace trunk2/x, which deletes trunk2/x/a
         *   svn rm trunk2/x
         *   mkdir trunk2/x
         *   svn add trunk2/x
         *   # "resurrect" trunk2/x/a
         *   svn copy trunk/x/a trunk2/x/a
         *
         * After these operations, we have a COPY_DELETED edge between trunk/x/a and trunk2/x/a because of the
         * deletion of trunk2/x being child of copied trunk2, and a COPY edge between trunk/x/a and trunk2/x/a
         * because of the explicit copy operation. Technically, there are two flows trunk2/x/a is being part of,
         * but the terminating flow is redundant.
         */
        fromNode.addDescendant(toNode, IFileHistoryEdge.Type.COPY);
        this.copyChildNodes(fromNode, toNode);
    }

    /**
     * Copies child nodes.
     *
     * @param fromParent The node the children of which are to be copied.
     * @param toParent The node where the children shall be copied to.
     */
    private void copyChildNodes(final ProxyableFileHistoryNode fromParent, final ProxyableFileHistoryNode toParent) {
        final String fromParentPath = fromParent.getFile().getPath();
        final IRevision fromRevision = fromParent.getFile().getRevision();
        final String toParentPath = toParent.getFile().getPath();
        final IRevision toRevision = toParent.getFile().getRevision();

        for (final ProxyableFileHistoryNode child : fromParent.getChildren()) {
            // don't copy deleted children
            if (!child.getType().equals(IFileHistoryNode.Type.DELETED)) {
                final String childPath = child.getFile().getPath();
                this.addCopy(
                        childPath,
                        toParentPath.concat(childPath.substring(fromParentPath.length())),
                        fromRevision,
                        toRevision);
            }
        }
    }

    /**
     * Adds a parent node to the node passed. Either an existing parent node is used or a new one is created.
     * @param node The node which to find/create parent node for.
     * @param inheritCopy If {@code true}, copy associations are inherited from parent node.
     */
    private void addParentNodes(final ProxyableFileHistoryNode node, final boolean inheritCopy) {
        final IRevisionedFile file = node.getFile();
        final String path = file.getPath();
        if (path.contains("/")) {
            final String parentPath = path.substring(0, path.lastIndexOf("/"));
            if (!parentPath.isEmpty()) {
                final IRevisionedFile fileRev = ChangestructureFactory.createFileInRevision(
                        parentPath,
                        file.getRevision());
                final ProxyableFileHistoryNode parent = this.getOrCreateConnectedNode(
                        fileRev,
                        node.isConfirmed() ? IFileHistoryNode.Type.CHANGED : IFileHistoryNode.Type.UNCONFIRMED);
                if (parent.isConfirmed() && !node.isConfirmed()) {
                    node.makeConfirmed();
                }
                parent.addChild(node);

                if (inheritCopy && parent.isCopyTarget() && !node.getType().equals(IFileHistoryNode.Type.ADDED)) {
                    final String name = path.substring(path.lastIndexOf("/") + 1);
                    final Set<ProxyableFileHistoryNode> ancestors = new LinkedHashSet<>();
                    for (final ProxyableFileHistoryEdge parentAncestorEdge : parent.getAncestors()) {
                        final ProxyableFileHistoryNode parentAncestor = parentAncestorEdge.getAncestor();
                        final IRevisionedFile parentAncestorRev = parentAncestor.getFile();
                        final IRevisionedFile ancestorRev = ChangestructureFactory.createFileInRevision(
                                parentAncestorRev.getPath() + "/" + name,
                                parentAncestorRev.getRevision());
                        final ProxyableFileHistoryNode ancestor =
                                this.getOrCreateConnectedNode(ancestorRev, IFileHistoryNode.Type.UNCONFIRMED);
                        ancestors.add(ancestor);
                    }
                    this.addNodeWithAncestors(node, ancestors, IFileHistoryEdge.Type.COPY);
                }
            }
        }
    }

    /**
     * Creates all missing children of passed node. The nodes are determined by recursively traversing the
     * node's ancestors.
     *
     * @param target The node the children of which are to be created.
     */
    private void createMissingChildren(final ProxyableFileHistoryNode target) {
        final Set<String> paths = new LinkedHashSet<>();
        for (final ProxyableFileHistoryNode child : target.getChildren()) {
            paths.add(child.getFile().getPath());
        }
        this.createMissingChildren(paths, target, target, new LinkedHashSet<ProxyableFileHistoryNode>());
        target.setHasAllChildren();
    }

    /**
     * Creates all missing children of passed node.
     *
     * @param paths The paths for which a child node has already been created.
     * @param source The node the children of which are to be computed.
     * @param target The node the children of which are to be created.
     * @param visitedNodes The set of nodes that have already been visited. As the file hierarchy graph is not
     *      necessarily a tree due to branching and merging, we need to mark nodes that have already been processed.
     */
    private void createMissingChildren(
            final Set<String> paths,
            final ProxyableFileHistoryNode source,
            final ProxyableFileHistoryNode target,
            final Set<ProxyableFileHistoryNode> visitedNodes) {

        final Deque<ProxyableFileHistoryNode> moreAncestors = new ArrayDeque<>();

        ProxyableFileHistoryNode node = source;
        while (node != null) {
            if (!visitedNodes.add(node)) {
                return;
            }

            if (!node.equals(target)) {
                this.createMissingTargetChildren(paths, node, target);
            }

            if (node.hasAllChildren()) {
                break;
            }

            boolean firstAncestor = true;
            for (final ProxyableFileHistoryEdge ancestorEdge : node.getAncestors()) {
                final ProxyableFileHistoryNode ancestor = ancestorEdge.getAncestor();
                if (ancestorEdge.getType().equals(IFileHistoryEdge.Type.NORMAL)) {
                    if (firstAncestor) {
                        node = ancestor;
                        firstAncestor = false;
                    } else {
                        moreAncestors.push(ancestor);
                    }
                }
            }
        }

        while (!moreAncestors.isEmpty()) {
            final ProxyableFileHistoryNode ancestor = moreAncestors.pop();
            this.createMissingChildren(paths, ancestor, target, visitedNodes);
        }
    }

    /**
     * Determines all missing child nodes of passed ancestor node and creates suitable descendant nodes as children of
     * passed target node. A child node is missing if the target node does not have a child node with the same path.
     *
     * @param paths The paths for which a child node has already been created.
     * @param ancestor The node the children of which are to be computed.
     * @param target The node the children of which are to be created.
     */
    private void createMissingTargetChildren(
            final Set<String> paths,
            final ProxyableFileHistoryNode ancestor,
            final ProxyableFileHistoryNode target) {

        for (final ProxyableFileHistoryNode ancestorChild : ancestor.getChildren()) {
            final String ancestorChildPath = ancestorChild.getFile().getPath();
            if (paths.add(ancestorChildPath)) {
                if (!ancestorChild.getType().equals(IFileHistoryNode.Type.DELETED)) {
                    final String targetChildPath = target.getFile().getPath() + ancestorChild.getPathRelativeToParent();
                    this.createTargetChild(target, ancestorChild, targetChildPath);
                }
            }
        }
    }

    /**
     * Creates a child node under a target node based on a child node of some ancestor of the target.
     *
     * @param target The node where the child node is to be created.
     * @param ancestorChild The child of the ancestor node that is to be recreated as child of passed target node.
     * @param targetChildPath The path of the new child node to be created.
     */
    private void createTargetChild(
            final ProxyableFileHistoryNode target,
            final ProxyableFileHistoryNode ancestorChild,
            final String targetChildPath) {
        final IRevisionedFile file =
                ChangestructureFactory.createFileInRevision(targetChildPath, target.getFile().getRevision());
        if (this.getNodeFor(file) == null) {
            final ProxyableFileHistoryNode targetChild = this.getOrCreateUnconnectedNode(
                    file,
                    IFileHistoryNode.Type.CHANGED);
            this.addNodeWithAncestors(targetChild, Collections.singleton(ancestorChild), IFileHistoryEdge.Type.NORMAL);
        }
    }

    /**
     * Returns or creates a {@link ProxyableFileHistoryNode} for a given {@link IRevisionedFile}
     * which inherits copy associations from its parent node if possible.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created as a node with passed type.
     * If the node returned is a root node, an artificial ancestor is created.
     */
    private ProxyableFileHistoryNode getOrCreateConnectedNode(
            final IRevisionedFile file,
            final IFileHistoryNode.Type nodeType) {
        return this.getOrCreateFileHistoryNode(file, nodeType, true, true);
    }

    /**
     * Returns or creates a {@link ProxyableFileHistoryNode} for a given {@link IRevisionedFile}
     * which does not inherit any copy associations from its parent node.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created as a node with passed type.
     * No artificial ancestor is created for root nodes.
     */
    private ProxyableFileHistoryNode getOrCreateUnconnectedNode(
            final IRevisionedFile file,
            final IFileHistoryNode.Type nodeType) {
        return this.getOrCreateFileHistoryNode(file, nodeType, false, false);
    }

    /**
     * Returns or creates a {@link ProxyableFileHistoryNode} for a given {@link IRevisionedFile}
     * which inherits copy associations from its parent node if possible.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created as a node with type
     * {@link IFileHistoryNode.Type#CHANGED}.
     * No artificial ancestor is created for root nodes.
     */
    private ProxyableFileHistoryNode getOrCreateChangeNode(final IRevisionedFile file) {
        return this.getOrCreateFileHistoryNode(file, IFileHistoryNode.Type.CHANGED, false, true);
    }

    /**
     * Returns or creates a {@link ProxyableFileHistoryNode} for a given {@link IRevisionedFile}.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created.
     * In addition, it is inserted into a possibly existing ancestor/descendant chain and/or parent/child of other
     * {@link ProxyableFileHistoryNode}s.
     *
     * @param file The revisioned file to get or create a {@link ProxyableFileHistoryNode} for.
     * @param nodeType The node type for a newly created node.
     * @param connected If {@code true}, an artificial ancestor node is created for a root node.
     * @param inheritCopy If {@code true}, copy associations are inherited from parent node if possible.
     */
    private ProxyableFileHistoryNode getOrCreateFileHistoryNode(
            final IRevisionedFile file,
            final IFileHistoryNode.Type nodeType,
            final boolean connected,
            final boolean inheritCopy) {

        // other types have to be set afterwards by using {@link ProxyableFileHistoryNode#makeDeleted()} or
        // {@link ProxyableFileHistoryNode#makeReplaced()}
        assert nodeType.equals(IFileHistoryNode.Type.UNCONFIRMED)
                || nodeType.equals(IFileHistoryNode.Type.ADDED)
                || nodeType.equals(IFileHistoryNode.Type.CHANGED);

        ProxyableFileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            node = new FileHistoryNode(this, file, nodeType);
            this.index.put(file.getPath(), node);

            this.addParentNodes(node, inheritCopy);

            if (node.isRoot() && connected) {
                final Set<ProxyableFileHistoryNode> ancestors = node.getType().equals(IFileHistoryNode.Type.ADDED)
                        ? new LinkedHashSet<>()
                        : this.findAncestorsFor(file);
                if (ancestors.isEmpty()) {
                    final IRevisionedFile alphaFile = ChangestructureFactory.createFileInRevision(
                            file.getPath(), ChangestructureFactory.createUnknownRevision(file.getRepository()));
                    if (!alphaFile.equals(file)) {
                        if (!node.getType().equals(IFileHistoryNode.Type.ADDED)) {
                            this.incompleteFlowStarts.put(file, node);
                        }
                        ancestors.add(this.getOrCreateUnconnectedNode(alphaFile, IFileHistoryNode.Type.UNCONFIRMED));
                    }
                }

                if (!ancestors.isEmpty()) {
                    this.addNodeWithAncestors(node, ancestors, IFileHistoryEdge.Type.NORMAL);
                }
            }
        }

        assert node != null;
        return node;
    }

    /**
     * Adds a node into the graph whose ancestor node has already been determined.
     *
     * @param node The node to add.
     * @param ancestor The ancestor node.
     * @param edgeType The edge to be inserted between the ancestor node and the node to add.
     */
    private void addNodeWithAncestors(
            final ProxyableFileHistoryNode node,
            final Set<ProxyableFileHistoryNode> ancestors,
            final IFileHistoryEdge.Type edgeType) {

        for (final ProxyableFileHistoryNode ancestor : ancestors) {
            if (ancestor.isConfirmed() && !node.isConfirmed()) {
                node.makeConfirmed();
            }

            if (node.isConfirmed()) {
                this.injectInteriorNode(ancestor, node);
            }

            ancestor.addDescendant(node, edgeType);
        }
    }

    /**
     * Injects a node into an existing ancestor/descendant relationship between other nodes. This can happen if the
     * interior node is created later due to copying an old file revision.
     *
     * @param ancestor The ancestor node.
     * @param interiorNode The interior node.
     */
    private void injectInteriorNode(
            final ProxyableFileHistoryNode ancestor,
            final ProxyableFileHistoryNode interiorNode) {

        if (!ancestor.getFile().getPath().equals(interiorNode.getFile().getPath())) {
            return;
        }

        final Iterator<? extends ProxyableFileHistoryEdge> it = ancestor.getDescendants().iterator();
        while (it.hasNext()) {
            final ProxyableFileHistoryEdge descendantOfAncestorEdge = it.next();
            // only inject interior node if edge is not a copy (this excludes rename/move operations)
            if (descendantOfAncestorEdge.getType().equals(IFileHistoryEdge.Type.NORMAL)) {
                final ProxyableFileHistoryNode descendantOfAncestor = descendantOfAncestorEdge.getDescendant();
                it.remove();
                descendantOfAncestor.removeAncestor(descendantOfAncestorEdge);
                interiorNode.addDescendant(
                        descendantOfAncestor,
                        descendantOfAncestorEdge.getType()); // always IFileHistoryEdge.Type.NORMAL
                this.incompleteFlowStarts.removeValue(descendantOfAncestor.getFile(), descendantOfAncestor);
            }
        }
    }

    @Override
    public final ProxyableFileHistoryNode getNodeFor(final IRevisionedFile file) {
        final List<ProxyableFileHistoryNode> nodesForKey = this.index.get(file.getPath());
        for (final ProxyableFileHistoryNode node : nodesForKey) {
            if (node.getFile().getRevision().equals(file.getRevision())) {
                return node;
            }
        }
        return null;
    }

    @Override
    public Set<ProxyableFileHistoryNode> findAncestorsFor(final IRevisionedFile file) {
        final List<ProxyableFileHistoryNode> nodesForKey = this.lookupFile(file);

        final Map<IRevisionedFile, ProxyableFileHistoryNode> ancestorNodes = nodesForKey.stream().filter(
                (final ProxyableFileHistoryNode node) ->
                    node.getFile().le(file) && !file.le(node.getFile()))
                .collect(Collectors.toMap(ProxyableFileHistoryNode::getFile, Function.identity()));

        final List<IRevisionedFile> maximalRevisions =
                PartialOrderAlgorithms.getAllMaximalElements(PartialOrderAlgorithms.topoSort(ancestorNodes.keySet()));

        final Set<ProxyableFileHistoryNode> result = maximalRevisions.stream()
                .map((final IRevisionedFile ancestorFile) -> ancestorNodes.get(ancestorFile))
                .filter((final ProxyableFileHistoryNode node) -> !node.getType().equals(IFileHistoryNode.Type.DELETED))
                .collect(Collectors.toSet());
        return result;
    }

    @Override
    public final Set<IFileHistoryNode> getIncompleteFlowStarts() {
        final Set<IFileHistoryNode> result = new LinkedHashSet<>();
        for (final IRevisionedFile file : this.incompleteFlowStarts.keySet()) {
            result.addAll(this.incompleteFlowStarts.get(file));
        }
        return result;
    }

    /**
     * Performs a file lookup in the index.
     * @param file The file to look for.
     * @return A list of matching {@link ProxyableFileHistoryNode}s.
     */
    protected final List<ProxyableFileHistoryNode> lookupFile(final IRevisionedFile file) {
        return this.index.get(file.getPath());
    }

    @Override
    public IDiffAlgorithm getDiffAlgorithm() {
        return this.diffAlgorithm;
    }

    @Override
    public String toString() {
        return this.index.toString();
    }
}
