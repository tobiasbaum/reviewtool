package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode.Type;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionVisitor;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;

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
        final FileHistoryNode node = this.getOrCreateFileHistoryNode(file, true, isNew, !isNew);
        if (node.isRoot()) {
            // for each root file within the history graph, we need an artificial ancestor node to record the changes
            final Set<IRevision> ancestorRevs;
            if (isNew) {
                // use an UnknownRevision node as ancestor for newly added nodes
                ancestorRevs = Collections.<IRevision>singleton(new UnknownRevision(revision.getRepository()));
            } else {
                ancestorRevs = ancestorRevisions;
            }

            for (final IRevision ancestorRevision : ancestorRevs) {
                final IRevisionedFile prevFile = ChangestructureFactory.createFileInRevision(path, ancestorRevision);
                final FileHistoryNode ancestor = this.getOrCreateFileHistoryNode(
                        prevFile,
                        true,   // must not exist
                        false,  // is not known to be a new node
                        false); // don't copy children (they do not exist anyway)
                this.addDescendants(ancestor, node);
            }
        }
    }

    /**
     * Adds an edge between ancestor and descendant. Child nodes are not copied.
     * Traverses the hierarchy upwards and adds all missing edges between the parents, if necessary.
     * @param ancestor The ancestor node.
     * @param descendant The descendant node.
     */
    private void addDescendants(final FileHistoryNode ancestor, final FileHistoryNode descendant) {
        assert ancestor.getFile().getPath().equals(descendant.getFile().getPath());
        FileHistoryNode ancestorNode = ancestor;
        FileHistoryNode descendantNode = descendant;
        while (!ancestorNode.hasDescendant(descendantNode)) {
            ancestorNode.addDescendant(descendantNode, new FileDiff(ancestor.getFile(), descendant.getFile()));
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

        final Set<FileHistoryNode> ancestors = new LinkedHashSet<>();
        for (final IRevision ancestorRevision : ancestorRevisions) {
            final IRevisionedFile ancestorFile =
                    ChangestructureFactory.createFileInRevision(path, ancestorRevision);
            final FileHistoryNode ancestor = this.getOrCreateFileHistoryNode(ancestorFile, false, false, true);
            assert !ancestor.getType().equals(Type.DELETED);
            ancestors.add(ancestor);
        }

        final IRevisionedFile file = ChangestructureFactory.createFileInRevision(path, revision);
        final FileHistoryNode node = this.getNodeFor(file);
        revision.accept(new IRevisionVisitor<Void>() {

            @Override
            public Void handleLocalRevision(final ILocalRevision revision) {
                if (node != null) {
                    assert !node.getType().equals(Type.DELETED);
                    node.makeDeleted();
                }
                return null;
            }

            @Override
            public Void handleRepoRevision(final IRepoRevision revision) {
                // a file in a non-local revision can have at most one associated node per revision
                assert node == null;
                return null;
            }

            @Override
            public Void handleUnknownRevision(final IUnknownRevision revision) {
                // it is not allowed to delete artificial nodes
                assert node == null;
                return null;
            }

        });

        if (node == null) {
            final FileHistoryNode deletionNode = new FileHistoryNode(file, Type.DELETED);
            this.addParentNodes(deletionNode, false, false);
            final Pair<String, IRepository> key = FileHistoryGraph.this.createKey(file);
            this.index.put(key, deletionNode);
            for (final FileHistoryNode ancestor : ancestors) {
                ancestor.addDescendant(deletionNode, new FileDiff(ancestor.getFile(), file));
                for (final FileHistoryNode child : ancestor.getChildren()) {
                    this.addDeletion(child.getFile().getPath(), revision,
                            Collections.singleton(ancestor.getFile().getRevision()));
                }
            }
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

        final FileHistoryNode fromNode = this.getOrCreateFileHistoryNode(fileFrom, false, false, true);
        final FileHistoryNode toNode = this.getOrCreateFileHistoryNode(fileTo, true, true, true);

        if (!fromNode.hasDescendant(toNode)) {
            this.addEdge(fromNode, toNode, true);
        }
    }

    /**
     * Adds an ancestor/descendant relationship between two nodes.
     * @param ancestor The ancestor.
     * @param descendant The descendant.
     * @param copyChildren If <code>true</code>, the children of the ancestor node are copied to the descendant node.
     */
    private void addEdge(final FileHistoryNode ancestor, final FileHistoryNode descendant,
            final boolean copyChildren) {
        ancestor.addDescendant(descendant, new FileDiff(ancestor.getFile(), descendant.getFile()));
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
                final FileHistoryNode parent = this.getOrCreateFileHistoryNode(fileRev, false, false, false);
                parent.addChild(node);

                if (!isNew && !node.isCopied() && parent.isCopied()) {
                    // special case: node begins lifetime with a change or deletion in a copied directory
                    final String name = path.substring(path.lastIndexOf("/") + 1);
                    for (final FileHistoryEdge parentAncestorEdge : parent.getAncestors()) {
                        final FileHistoryNode parentAncestor = parentAncestorEdge.getAncestor();
                        final IRevisionedFile parentAncestorRev = parentAncestor.getFile();
                        final IRevisionedFile ancestorRev = ChangestructureFactory.createFileInRevision(
                                parentAncestorRev.getPath() + "/" + name,
                                parentAncestorRev.getRevision());
                        final FileHistoryNode ancestor =
                                this.getOrCreateFileHistoryNode(ancestorRev, false, false, false);
                        this.addEdge(ancestor, node, copyChildren);
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
     * @param mustNotExist If <code>true</code>, the node may not already exist.
     * @param isNew If <code>true</code>, the node is known to have been added in passed revision. This makes a
     *      difference when the node's parent is a copied directory: New nodes remain root nodes, while other nodes
     *      will be associated to an ancestor in the parent's copy source.
     * @param copyChildren If <code>true</code> and a new node is created, child nodes are copied from the ancestor
     *      (if it exists) to the new node.
     */
    private FileHistoryNode getOrCreateFileHistoryNode(final IRevisionedFile file, final boolean mustNotExist,
            final boolean isNew, final boolean copyChildren) {
        FileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            final FileHistoryNode newNode = new FileHistoryNode(file, Type.NORMAL);
            this.index.put(this.createKey(file), newNode);

            this.addParentNodes(newNode, isNew, copyChildren);
            if (!isNew && newNode.isRoot()) { // addParentNodes() may have already added an ancestor
                final FileHistoryNode ancestor = this.findAncestorFor(file);
                if (ancestor != null) {
                    this.injectInteriorNode(ancestor, newNode);
                    this.addEdge(ancestor, newNode, copyChildren);
                }
            }

            return newNode;
        } else {
            assert !isNew && !mustNotExist;
            return node;
        }
    }

    /**
     * Injects a node into an existing ancestor/descendant relationship between other nodes. This can happen if the
     * interior node is created later due to copying an old file revision.
     *
     * @param ancestor The ancestor.
     * @param descendant The descendant.
     */
    private void injectInteriorNode(final FileHistoryNode ancestor, final FileHistoryNode descendant) {
        final Iterator<FileHistoryEdge> it = ancestor.getDescendants().iterator();
        while (it.hasNext()) {
            final FileHistoryEdge descendantOfAncestorEdge = it.next();
            final FileHistoryNode descendantOfAncestor = descendantOfAncestorEdge.getDescendant();
            // only inject interior node if it's the same path (i.e. no rename/move)
            if (ancestor.getFile().getPath().equals(descendantOfAncestor.getFile().getPath())) {
                it.remove();
                descendantOfAncestor.removeAncestor(descendantOfAncestorEdge);
                descendant.addDescendant(descendantOfAncestor, descendantOfAncestorEdge.getDiff());
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
