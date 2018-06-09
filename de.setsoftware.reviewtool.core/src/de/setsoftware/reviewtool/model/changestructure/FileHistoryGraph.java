package de.setsoftware.reviewtool.model.changestructure;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
public abstract class FileHistoryGraph extends AbstractFileHistoryGraph implements IMutableFileHistoryGraph {

    private final Multimap<Pair<String, IRepository>, FileHistoryNode> index = new Multimap<>();

    @Override
    public final boolean contains(final String path, final IRepository repo) {
        return !this.index.get(Pair.create(path, repo)).isEmpty();
    }

    @Override
    public final void addAddition(
            final String path,
            final IRevision revision) {

        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        this.getOrCreateConnectedNode(file, IFileHistoryNode.Type.ADDED);
    }

    @Override
    public final void addChange(
            final String path,
            final IRevision revision,
            Set<? extends IRevision> ancestorRevisions) {

        assert !ancestorRevisions.isEmpty();
        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        final FileHistoryNode node = this.getOrCreateChangeNode(file);
        assert !node.getType().equals(IFileHistoryNode.Type.DELETED);

        if (node.isRoot()) {
            // for each root file within the history graph, we need an ancestor node to record the changes
            for (final IRevision ancestorRevision : ancestorRevisions) {
                final IRevisionedFile prevFile = ChangestructureFactory.createFileInRevision(path, ancestorRevision);
                final FileHistoryNode ancestor = this.getOrCreateConnectedNode(
                        prevFile,
                        IFileHistoryNode.Type.UNCONFIRMED);
                addEdge(ancestor, node, IFileHistoryEdge.Type.NORMAL);
            }
        }
        //else: a change is being recorded for a node copied in the same commit, so passed ancestors can be ignored
    }

    @Override
    public final void addDeletion(
            final String path,
            final IRevision revision) {

        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        final FileHistoryNode node = this.getOrCreateConnectedNode(file, IFileHistoryNode.Type.CHANGED);
        node.makeDeleted();

        this.createMissingChildren(node);
        for (final FileHistoryNode child : node.getChildren()) {
            final IRevisionedFile childFile = child.getFile();
            assert revision.equals(childFile.getRevision());
            this.addDeletion(childFile.getPath(), revision);
        }
    }

    @Override
    public final void addReplacement(
            final String path,
            final IRevision revision) {

        this.addDeletion(path, revision);
        this.addAddition(path, revision);
    }

    @Override
    public final void addReplacement(
            final String path,
            final IRevision revision,
            final String pathFrom,
            final IRevision revisionFrom) {

        this.addDeletion(path, revision);
        this.addCopy(pathFrom, path, revisionFrom, revision, IFileHistoryNode.Type.ADDED);
    }

    @Override
    public final void addCopy(
            final String pathFrom,
            final String pathTo,
            final IRevision revisionFrom,
            final IRevision revisionTo) {
        this.addCopy(pathFrom, pathTo, revisionFrom, revisionTo, IFileHistoryNode.Type.CHANGED);
    }

    private void addCopy(
            final String pathFrom,
            final String pathTo,
            final IRevision revisionFrom,
            final IRevision revisionTo,
            final IFileHistoryNode.Type nodeType) {

        final IRevisionedFile fileFrom = ChangestructureFactory.createFileInRevision(pathFrom, revisionFrom);
        final IRevisionedFile fileTo = ChangestructureFactory.createFileInRevision(pathTo, revisionTo);

        final FileHistoryNode fromNode = this.getOrCreateConnectedNode(fileFrom, IFileHistoryNode.Type.UNCONFIRMED);
        final FileHistoryNode toNode = this.getOrCreateUnconnectedNode(fileTo, nodeType);

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
        addEdge(fromNode, toNode, IFileHistoryEdge.Type.COPY);
        this.copyChildNodes(fromNode, toNode);
    }

    /**
     * Copies child nodes.
     *
     * @param fromParent The node the children of which are to be copied.
     * @param toParent The node where the children shall be copied to.
     */
    private void copyChildNodes(final FileHistoryNode fromParent, final FileHistoryNode toParent) {
        final String fromParentPath = fromParent.getFile().getPath();
        final IRevision fromRevision = fromParent.getFile().getRevision();
        final String toParentPath = toParent.getFile().getPath();
        final IRevision toRevision = toParent.getFile().getRevision();

        for (final FileHistoryNode child : fromParent.getChildren()) {
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
     * Adds an ancestor/descendant relationship between two nodes.
     * @param ancestor The ancestor.
     * @param descendant The descendant.
     * @param type The type of the edge to create.
     */
    private static void addEdge(
            final FileHistoryNode ancestor,
            final FileHistoryNode descendant,
            final IFileHistoryEdge.Type type) {
        ancestor.addDescendant(descendant, type, new FileDiff(ancestor.getFile(), descendant.getFile()));
    }

    /**
     * Adds a parent node to the node passed. Either an existing parent node is used or a new one is created.
     * @param node The node which to find/create parent node for.
     * @param inheritCopy If {@code true}, copy associations are inherited from parent node.
     */
    private void addParentNodes(final FileHistoryNode node, final boolean inheritCopy) {
        final IRevisionedFile file = node.getFile();
        final String path = file.getPath();
        if (path.contains("/")) {
            final String parentPath = path.substring(0, path.lastIndexOf("/"));
            if (!parentPath.isEmpty()) {
                final IRevisionedFile fileRev = ChangestructureFactory.createFileInRevision(
                        parentPath,
                        file.getRevision());
                final FileHistoryNode parent = this.getOrCreateConnectedNode(
                        fileRev,
                        node.isConfirmed() ? IFileHistoryNode.Type.CHANGED : IFileHistoryNode.Type.UNCONFIRMED);
                if (parent.isConfirmed() && !node.isConfirmed()) {
                    node.makeConfirmed();
                }
                parent.addChild(node);

                if (inheritCopy && parent.isCopyTarget() && !node.getType().equals(IFileHistoryNode.Type.ADDED)) {
                    final String name = path.substring(path.lastIndexOf("/") + 1);
                    for (final FileHistoryEdge parentAncestorEdge : parent.getAncestors()) {
                        final FileHistoryNode parentAncestor = parentAncestorEdge.getAncestor();
                        final IRevisionedFile parentAncestorRev = parentAncestor.getFile();
                        final IRevisionedFile ancestorRev = ChangestructureFactory.createFileInRevision(
                                parentAncestorRev.getPath() + "/" + name,
                                parentAncestorRev.getRevision());
                        final FileHistoryNode ancestor =
                                this.getOrCreateConnectedNode(ancestorRev, IFileHistoryNode.Type.UNCONFIRMED);
                        this.addNodeWithAncestor(node, ancestor, IFileHistoryEdge.Type.COPY);
                    }
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
    private void createMissingChildren(final FileHistoryNode target) {
        final Set<String> paths = new LinkedHashSet<>();
        for (final FileHistoryNode child : target.getChildren()) {
            paths.add(child.getFile().getPath());
        }
        this.createMissingChildren(paths, target, target, new LinkedHashSet<FileHistoryNode>());
    }

    /**
     * Creates all missing children of passed node.
     *
     * @param paths The paths for which a child node has already been created.
     * @param node The node the children of which are to be computed.
     * @param target The node the children of which are to be created.
     * @param visitedNodes The set of nodes that have already been visited. As the file hierarchy graph is not
     *      necessarily a tree due to branching and merging, we need to mark nodes that have already been processed.
     */
    private void createMissingChildren(
            final Set<String> paths,
            final FileHistoryNode node,
            final FileHistoryNode target,
            final Set<FileHistoryNode> visitedNodes) {

        if (visitedNodes.contains(node)) {
            return;
        } else {
            visitedNodes.add(node);
        }

        this.createMissingTargetChildren(paths, node, target);

        for (final FileHistoryEdge ancestorEdge : node.getAncestors()) {
            final FileHistoryNode ancestor = ancestorEdge.getAncestor();
            if (ancestorEdge.getType().equals(IFileHistoryEdge.Type.NORMAL)) {
                this.createMissingChildren(paths, ancestor, target, visitedNodes);
            }
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
            final FileHistoryNode ancestor,
            final FileHistoryNode target) {

        for (final FileHistoryNode ancestorChild : ancestor.getChildren()) {
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
            final FileHistoryNode target,
            final FileHistoryNode ancestorChild,
            final String targetChildPath) {
        final IRevisionedFile file =
                ChangestructureFactory.createFileInRevision(targetChildPath, target.getFile().getRevision());
        if (this.getNodeFor(file) == null) {
            final FileHistoryNode targetChild = this.getOrCreateUnconnectedNode(
                    file,
                    IFileHistoryNode.Type.CHANGED);
            this.addNodeWithAncestor(targetChild, ancestorChild, IFileHistoryEdge.Type.NORMAL);
        }
    }

    /**
     * Returns or creates a {@link FileHistoryNode} for a given {@link IRevisionedFile}
     * which inherits copy associations from its parent node if possible.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created as a node with passed type.
     * If the node returned is a root node, an artificial ancestor is created.
     */
    private FileHistoryNode getOrCreateConnectedNode(
            final IRevisionedFile file,
            final IFileHistoryNode.Type nodeType) {
        return this.getOrCreateFileHistoryNode(file, nodeType, true, true);
    }

    /**
     * Returns or creates a {@link FileHistoryNode} for a given {@link IRevisionedFile}
     * which does not inherit any copy associations from its parent node.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created as a node with passed type.
     * No artificial ancestor is created for root nodes.
     */
    private FileHistoryNode getOrCreateUnconnectedNode(
            final IRevisionedFile file,
            final IFileHistoryNode.Type nodeType) {
        return this.getOrCreateFileHistoryNode(file, nodeType, false, false);
    }

    /**
     * Returns or creates a {@link FileHistoryNode} for a given {@link IRevisionedFile}
     * which inherits copy associations from its parent node if possible.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created as a node with type
     * {@link IFileHistoryNode.Type#CHANGED}.
     * No artificial ancestor is created for root nodes.
     */
    private FileHistoryNode getOrCreateChangeNode(final IRevisionedFile file) {
        return this.getOrCreateFileHistoryNode(file, IFileHistoryNode.Type.CHANGED, false, true);
    }

    /**
     * Returns or creates a {@link FileHistoryNode} for a given {@link IRevisionedFile}.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created.
     * In addition, it is inserted into a possibly existing ancestor/descendant chain and/or parent/child of other
     * {@link FileHistoryNode}s.
     *
     * @param file The revisioned file to get or create a {@link FileHistoryNode} for.
     * @param nodeType The node type for a newly created node.
     * @param connected If {@code true}, an artificial ancestor node is created for a root node.
     * @param inheritCopy If {@code true}, copy associations are inherited from parent node if possible.
     */
    private FileHistoryNode getOrCreateFileHistoryNode(
            final IRevisionedFile file,
            final IFileHistoryNode.Type nodeType,
            final boolean connected,
            final boolean inheritCopy) {

        // other types have to be set afterwards by using {@link FileHistoryNode#makeDeleted()} or
        // {@link FileHistoryNode#makeReplaced()}
        assert nodeType.equals(IFileHistoryNode.Type.UNCONFIRMED)
                || nodeType.equals(IFileHistoryNode.Type.ADDED)
                || nodeType.equals(IFileHistoryNode.Type.CHANGED);

        FileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            node = new FileHistoryNode(this, file, nodeType);
            this.index.put(this.createKey(file), node);

            this.addParentNodes(node, inheritCopy);

            if (node.isRoot() && connected) {
                FileHistoryNode ancestor = node.getType().equals(IFileHistoryNode.Type.ADDED) ? null
                        : this.findAncestorFor(file);
                if (ancestor == null) {
                    final IRevisionedFile alphaFile = ChangestructureFactory.createFileInRevision(
                            file.getPath(), ChangestructureFactory.createUnknownRevision(file.getRepository()));
                    if (!alphaFile.equals(file)) {
                        ancestor = this.getOrCreateUnconnectedNode(alphaFile, IFileHistoryNode.Type.UNCONFIRMED);
                    }
                }

                if (ancestor != null) {
                    this.addNodeWithAncestor(node, ancestor, IFileHistoryEdge.Type.NORMAL);
                }
            }
        } else {
            if (nodeType.equals(IFileHistoryNode.Type.ADDED)) {
                node.makeReplaced();
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
    private void addNodeWithAncestor(
            final FileHistoryNode node,
            final FileHistoryNode ancestor,
            final IFileHistoryEdge.Type edgeType) {

        if (ancestor.isConfirmed() && !node.isConfirmed()) {
            node.makeConfirmed();
        }

        if (node.isConfirmed()) {
            this.injectInteriorNode(ancestor, node);
        }

        addEdge(ancestor, node, edgeType);
    }

    /**
     * Injects a node into an existing ancestor/descendant relationship between other nodes. This can happen if the
     * interior node is created later due to copying an old file revision.
     *
     * @param ancestor The ancestor node.
     * @param interiorNode The interior node.
     */
    private void injectInteriorNode(final FileHistoryNode ancestor, final FileHistoryNode interiorNode) {
        if (!ancestor.getFile().getPath().equals(interiorNode.getFile().getPath())) {
            return;
        }

        final Iterator<FileHistoryEdge> it = ancestor.getDescendants().iterator();
        while (it.hasNext()) {
            final FileHistoryEdge descendantOfAncestorEdge = it.next();
            // only inject interior node if edge is not a copy (this excludes rename/move operations)
            if (descendantOfAncestorEdge.getType().equals(IFileHistoryEdge.Type.NORMAL)) {
                final FileHistoryNode descendantOfAncestor = descendantOfAncestorEdge.getDescendant();
                it.remove();
                descendantOfAncestor.removeAncestor(descendantOfAncestorEdge);
                interiorNode.addDescendant(
                        descendantOfAncestor,
                        descendantOfAncestorEdge.getType(), // always IFileHistoryEdge.Type.NORMAL
                        descendantOfAncestorEdge.getDiff());
            }
        }
    }

    @Override
    public final FileHistoryNode getNodeFor(final IRevisionedFile file) {
        final Pair<String, IRepository> key = this.createKey(file);
        final List<FileHistoryNode> nodesForKey = this.index.get(key);
        for (final FileHistoryNode node : nodesForKey) {
            if (node.getFile().getRevision().equals(file.getRevision())) {
                return node;
            }
        }
        return null;
    }

    private Pair<String, IRepository> createKey(final IRevisionedFile file) {
        return Pair.create(file.getPath(), file.getRepository());
    }

    /**
     * Performs a file lookup in the index.
     * @param file The file to look for.
     * @return A list of matching {@link FileHistoryNode}s.
     */
    protected final List<FileHistoryNode> lookupFile(final IRevisionedFile file) {
        final Pair<String, IRepository> key = this.createKey(file);
        return this.index.get(key);
    }

    /**
     * Returns the nearest ancestor for passed {@link IRevisionedFile} having the same path, or <code>null</code>
     * if no suitable node exists. To be suitable, the ancestor node must not be deleted.
     */
    public abstract FileHistoryNode findAncestorFor(IRevisionedFile file);

    @Override
    public String toString() {
        return this.index.toString();
    }
}
