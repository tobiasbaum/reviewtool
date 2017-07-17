package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode.Type;
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
    public final void addAdditionOrChange(
            final String path,
            final IRevision revision,
            final Set<IRevision> ancestorRevisions) {

        final boolean isNew = ancestorRevisions.isEmpty();
        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        final FileHistoryNode node = this.getOrCreateFileHistoryNode(file, isNew, !isNew);
        if (node.getType().equals(Type.DELETED)) {
            assert isNew;
            node.makeReplaced();
        }

        // if a node is a copy target, no ancestor has to be created and associated because it already exists
        if (!node.isCopyTarget()) {
            for (final IRevision ancestorRevision : ancestorRevisions) {
                final IRevisionedFile prevFile = ChangestructureFactory.createFileInRevision(path, ancestorRevision);
                final FileHistoryNode ancestor = this.getOrCreateFileHistoryNode(
                        prevFile,
                        false,  // is not known to be a new node
                        false); // don't copy children (they do not exist anyway)
                this.addDescendants(ancestor, node, IFileHistoryEdge.Type.NORMAL);
            }
        }
    }

    /**
     * Adds an edge between ancestor and descendant. Child nodes are not copied.
     * Traverses the hierarchy upwards and adds all missing edges between the parents, if necessary.
     * @param ancestor The ancestor node.
     * @param descendant The descendant node.
     * @param type The type of the edges to add.
     */
    private void addDescendants(
            final FileHistoryNode ancestor,
            final FileHistoryNode descendant,
            final IFileHistoryEdge.Type type) {
        assert ancestor.getFile().getPath().equals(descendant.getFile().getPath());
        FileHistoryNode ancestorNode = ancestor;
        FileHistoryNode descendantNode = descendant;
        while (!ancestorNode.hasDescendant(descendantNode)) {
            ancestorNode.addDescendant(descendantNode, type, new FileDiff(ancestor.getFile(), descendant.getFile()));
            assert ancestorNode.hasParent() == descendantNode.hasParent();
            if (!ancestorNode.hasParent()) {
                return;
            }
            ancestorNode = ancestorNode.getParent();
            descendantNode = descendantNode.getParent();
        }
    }

    @Override
    public final void addDeletion(
            final String path,
            final IRevision revision,
            final Set<IRevision> ancestorRevisions) {

        assert !ancestorRevisions.isEmpty();
        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        final FileHistoryNode node = this.getNodeFor(file);
        if (node != null) {
            node.makeDeleted();
        } else {
            final FileHistoryNode deletionNode = new FileHistoryNode(file, Type.DELETED);
            this.addParentNodes(deletionNode, false, false);
            final Pair<String, IRepository> key = this.createKey(file);
            this.index.put(key, deletionNode);

            if (deletionNode.isRoot()) { // addParentNodes() may have already added ancestors
                for (final IRevision ancestorRevision : ancestorRevisions) {
                    final IRevisionedFile ancestorFile =
                            ChangestructureFactory.createFileInRevision(path, ancestorRevision);
                    final FileHistoryNode ancestor = this.getOrCreateFileHistoryNode(ancestorFile, false, true);
                    assert !ancestor.getType().equals(Type.DELETED);

                    this.addDescendants(ancestor, deletionNode, IFileHistoryEdge.Type.NORMAL);

                    for (final FileHistoryNode child : ancestor.getChildren()) {
                        if (!child.getType().equals(Type.DELETED)) {
                            this.addDeletion(child.getFile().getPath(), revision,
                                    Collections.singleton(ancestor.getFile().getRevision()));
                        }
                    }
                }
            }
        }
    }

    @Override
    public final void addReplacement(
            final String path,
            final IRevision revision,
            final Set<IRevision> ancestorRevisions) {

        this.addDeletion(path, revision, ancestorRevisions);
        this.addAdditionOrChange(path, revision, Collections.<IRevision> emptySet());
    }

    @Override
    public final void addReplacement(
            final String path,
            final IRevision revision,
            final Set<IRevision> ancestorRevisions,
            final String pathFrom,
            final IRevision revisionFrom) {

        this.addDeletion(path, revision, ancestorRevisions);
        this.addCopy(pathFrom, path, revisionFrom, revision);
    }

    @Override
    public final void addCopy(
            final String pathFrom,
            final String pathTo,
            final IRevision revisionFrom,
            final IRevision revisionTo) {

        final IRevisionedFile fileFrom = ChangestructureFactory.createFileInRevision(pathFrom, revisionFrom);
        final IRevisionedFile fileTo = ChangestructureFactory.createFileInRevision(pathTo, revisionTo);

        final FileHistoryNode fromNode = this.getOrCreateFileHistoryNode(fileFrom, false, true);
        final FileHistoryNode toNode = this.getOrCreateFileHistoryNode(fileTo, true, true);
        if (toNode.getType().equals(Type.DELETED)) {
            toNode.makeReplaced();
        }

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
        this.addEdge(fromNode, toNode, IFileHistoryEdge.Type.COPY, true);
    }

    /**
     * Adds an ancestor/descendant relationship between two nodes.
     * @param ancestor The ancestor.
     * @param descendant The descendant.
     * @param type The type of the edge to create.
     * @param copyChildren If <code>true</code>, the children of the ancestor node are copied to the descendant node.
     */
    private void addEdge(
            final FileHistoryNode ancestor,
            final FileHistoryNode descendant,
            final IFileHistoryEdge.Type type,
            final boolean copyChildren) {
        ancestor.addDescendant(descendant, type, new FileDiff(ancestor.getFile(), descendant.getFile()));
        if (copyChildren) {
            this.copyChildNodes(ancestor, descendant);
        }
    }

    /**
     * Adds a parent node to the node passed. Either an existing parent node is used or a new one is created.
     * @param node The node which to find/create parent node for.
     * @param isNew If <code>true</code>, it is known that the node has been added in its revision.
     */
    private void addParentNodes(final FileHistoryNode node, final boolean isNew, final boolean copyChildren) {
        final IRevisionedFile file = node.getFile();
        final String path = file.getPath();
        if (path.contains("/")) {
            final String parentPath = path.substring(0, path.lastIndexOf("/"));
            if (!parentPath.isEmpty()) {
                final IRevisionedFile fileRev = ChangestructureFactory.createFileInRevision(
                        parentPath,
                        file.getRevision());
                // don't copy child nodes when traversing upwards the tree as they already exist
                final FileHistoryNode parent = this.getOrCreateFileHistoryNode(fileRev, false, false);
                parent.addChild(node);

                if (!isNew && parent.isCopyTarget()) {
                    // special case: node begins lifetime with a change or deletion in a copied directory
                    final String name = path.substring(path.lastIndexOf("/") + 1);
                    for (final FileHistoryEdge parentAncestorEdge : parent.getAncestors()) {
                        final FileHistoryNode parentAncestor = parentAncestorEdge.getAncestor();
                        final IRevisionedFile parentAncestorRev = parentAncestor.getFile();
                        final IRevisionedFile ancestorRev = ChangestructureFactory.createFileInRevision(
                                parentAncestorRev.getPath() + "/" + name,
                                parentAncestorRev.getRevision());
                        final FileHistoryNode ancestor =
                                this.getOrCreateFileHistoryNode(ancestorRev, false, false);
                        this.addEdge(ancestor, node,
                                node.getType().equals(Type.DELETED) ? IFileHistoryEdge.Type.COPY_DELETED
                                        : IFileHistoryEdge.Type.COPY,
                                copyChildren);
                    }
                }
            }
        }
    }

    private static String crateCopyTargetName(final String childPath, final String oldParentPath,
            final String newParentPath) {
        return newParentPath.concat(childPath.substring(oldParentPath.length()));
    }

    /**
     * Copies child nodes.
     */
    private void copyChildNodes(final FileHistoryNode oldNode, final FileHistoryNode newNode) {
        final String oldParentPath = oldNode.getFile().getPath();
        final IRevision oldRevision = oldNode.getFile().getRevision();
        final String newParentPath = newNode.getFile().getPath();
        final IRevision newRevision = newNode.getFile().getRevision();

        for (final FileHistoryNode child : oldNode.getChildren()) {
            // don't copy deleted children
            if (!child.getType().equals(Type.DELETED)) {
                final String childPath = child.getFile().getPath();
                this.addCopy(childPath,
                        crateCopyTargetName(childPath, oldParentPath, newParentPath),
                        oldRevision, newRevision);
            }
        }
    }

    /**
     * Returns or creates a {@link FileHistoryNode} for a given {@link IRevisionedFile}.
     * If a node for that {@link IRevisionedFile} already exists, it is returned.
     * If a node for that {@link IRevisionedFile} does not exist, it is created.
     * In addition, it is inserted into a possibly existing ancestor/descendant chain and/or parent/child of other
     * {@link FileHistoryNode}s.
     *
     * @param isNew If <code>true</code>, the node is known to have been added in passed revision. This makes a
     *      difference when the node's parent is a copied directory: New nodes remain root nodes, while other nodes
     *      will be associated to an ancestor in the parent's copy source.
     * @param copyChildren If <code>true</code> and a new node is created, child nodes are copied from the ancestor
     *      (if it exists) to the new node.
     */
    private FileHistoryNode getOrCreateFileHistoryNode(
            final IRevisionedFile file,
            final boolean isNew,
            final boolean copyChildren) {
        FileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            final FileHistoryNode newNode = new FileHistoryNode(file, Type.NORMAL);
            this.index.put(this.createKey(file), newNode);

            this.addParentNodes(newNode, isNew, copyChildren);
            if (!isNew && newNode.isRoot()) { // addParentNodes() may have already added an ancestor
                final FileHistoryNode ancestor = this.findAncestorFor(file);
                if (ancestor != null) {
                    this.injectInteriorNode(ancestor, newNode);
                    this.addEdge(ancestor, newNode, IFileHistoryEdge.Type.NORMAL, copyChildren);
                }
            }

            return newNode;
        } else {
            assert !isNew || node.getType().equals(Type.DELETED);
            return node;
        }
    }

    /**
     * Injects a node into an existing ancestor/descendant relationship between other nodes. This can happen if the
     * interior node is created later due to copying an old file revision.
     *
     * @param ancestor The ancestor node.
     * @param interiorNode The interior node.
     */
    private void injectInteriorNode(final FileHistoryNode ancestor, final FileHistoryNode interiorNode) {
        final Iterator<FileHistoryEdge> it = ancestor.getDescendants().iterator();
        while (it.hasNext()) {
            final FileHistoryEdge descendantOfAncestorEdge = it.next();
            // only inject interior node if edge is not a copy (this includes rename/move operations)
            if (descendantOfAncestorEdge.getType().equals(IFileHistoryEdge.Type.NORMAL)) {
                final FileHistoryNode descendantOfAncestor = descendantOfAncestorEdge.getDescendant();
                assert ancestor.getFile().getPath().equals(descendantOfAncestor.getFile().getPath());

                it.remove();
                descendantOfAncestor.removeAncestor(descendantOfAncestorEdge);
                interiorNode.addDescendant(descendantOfAncestor, IFileHistoryEdge.Type.NORMAL,
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
