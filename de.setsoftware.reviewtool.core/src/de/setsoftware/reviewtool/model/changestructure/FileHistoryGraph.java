package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Pair;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
public abstract class FileHistoryGraph implements IFileHistoryGraph {

    private final Multimap<Pair<String, Repository>, FileHistoryNode> index = new Multimap<>();

    /**
     * Returns true if passed path is known to this {@link IFileHistoryGraph}.
     * @param path The path to check.
     * @param repo The repository.
     * @return <code>true</code> if the path is known, else <code>false</code>
     */
    public final boolean contains(final String path, final Repository repo) {
        return !this.index.get(Pair.create(path, repo)).isEmpty();
    }

    /**
     * Adds the information that the file with the given path was added or changed at the commit of the given revision.
     */
    public final void addAdditionOrChange(
            final String path,
            final Revision revision,
            final Set<Revision> ancestorRevisions,
            final Repository repo) {

        final boolean isNew = ancestorRevisions.isEmpty();
        final FileInRevision file = ChangestructureFactory.createFileInRevision(path, revision, repo);
        final FileHistoryNode node = this.getOrCreateFileHistoryNode(file, true, isNew, !isNew);
        if (node.isRoot()) {
            // for each root file within the history graph, we need an artificial ancestor node to record the changes
            final Set<Revision> ancestorRevs;
            if (isNew) {
                // use an UnknownRevision node as ancestor for newly added nodes
                ancestorRevs = Collections.<Revision>singleton(new UnknownRevision());
            } else {
                ancestorRevs = ancestorRevisions;
            }

            for (final Revision ancestorRevision : ancestorRevs) {
                final FileInRevision prevFile =
                        ChangestructureFactory.createFileInRevision(path, ancestorRevision, repo);
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

    /**
     * Adds the information that the file with the given path was deleted with the commit of the given revision.
     * If ancestor nodes exist, the deletion node of type {@link NonExistingFileHistoryNode} is linked to them,
     * possibly creating intermediate {@link FileHistoryNode}s just before the deletion. This supports
     * finding the last revision of a file before being deleted.
     */
    public final void addDeletion(
            final String path,
            final Revision revision,
            final Set<Revision> ancestorRevisions,
            final Repository repo) {

        final Set<FileHistoryNode> ancestors = new LinkedHashSet<>();
        for (final Revision ancestorRevision : ancestorRevisions) {
            final FileInRevision ancestorFile =
                    ChangestructureFactory.createFileInRevision(path, ancestorRevision, repo);
            final FileHistoryNode ancestor = this.getOrCreateFileHistoryNode(ancestorFile, false, false, true);
            assert !ancestor.isDeleted();
            ancestors.add(ancestor);
        }

        final FileInRevision file = ChangestructureFactory.createFileInRevision(path, revision, repo);
        final FileHistoryNode node = this.getNodeFor(file);
        revision.accept(new RevisionVisitor<Void>() {

            @Override
            public Void handleLocalRevision(final LocalRevision revision) {
                if (node != null) {
                    assert !node.isDeleted();
                    node.setDeleted(true);
                }
                return null;
            }

            @Override
            public Void handleRepoRevision(final RepoRevision revision) {
                // a file in a non-local revision can have at most one associated node per revision
                assert node == null;
                return null;
            }

            @Override
            public Void handleUnknownRevision(final UnknownRevision revision) {
                // it is not allowed to delete artificial nodes
                assert node == null;
                return null;
            }

        });

        if (node == null) {
            final FileHistoryNode deletionNode = new FileHistoryNode(file, true);
            this.addParentNodes(deletionNode, false, false);
            final Pair<String, Repository> key = FileHistoryGraph.this.createKey(file);
            this.index.put(key, deletionNode);
            for (final FileHistoryNode ancestor : ancestors) {
                ancestor.addDescendant(deletionNode, new FileDiff(ancestor.getFile(), file));
                for (final FileHistoryNode child : ancestor.getChildren()) {
                    this.addDeletion(child.getFile().getPath(), revision,
                            Collections.singleton(ancestor.getFile().getRevision()), repo);
                }
            }
        }
    }

    /**
     * Adds the information that the file with the given "from" path was copied with the commit of the given revision
     * to the given "to" path.
     */
    public final void addCopy(
            final String pathFrom,
            final String pathTo,
            final Revision revisionFrom,
            final Revision revisionTo,
            final Repository repo) {

        final FileInRevision fileFrom = ChangestructureFactory.createFileInRevision(pathFrom, revisionFrom, repo);
        final FileInRevision fileTo = ChangestructureFactory.createFileInRevision(pathTo, revisionTo, repo);

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
        final FileInRevision file = node.getFile();
        final String path = file.getPath();
        if (path.contains("/")) {
            final String parentPath = path.substring(0, path.lastIndexOf("/"));
            if (!parentPath.isEmpty()) {
                final FileInRevision fileRev = ChangestructureFactory.createFileInRevision(
                        parentPath,
                        file.getRevision(),
                        file.getRepository());
                // don't copy child nodes when traversing upwards the tree as they already exist
                final FileHistoryNode parent = this.getOrCreateFileHistoryNode(fileRev, false, false, false);
                parent.addChild(node);

                if (!isNew && !node.isCopied() && parent.isCopied()) {
                    // special case: node begins lifetime with a change or deletion in a copied directory
                    final String name = path.substring(path.lastIndexOf("/") + 1);
                    for (final FileHistoryEdge parentAncestorEdge : parent.getAncestors()) {
                        final FileHistoryNode parentAncestor = parentAncestorEdge.getAncestor();
                        final FileInRevision parentAncestorRev = parentAncestor.getFile();
                        final FileInRevision ancestorRev = ChangestructureFactory.createFileInRevision(
                                parentAncestorRev.getPath() + "/" + name,
                                parentAncestorRev.getRevision(),
                                parentAncestorRev.getRepository());
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
        final Revision oldRevision = oldNode.getFile().getRevision();
        final String newParentPath = newNode.getFile().getPath();
        final Revision newRevision = newNode.getFile().getRevision();
        final Repository repository = oldNode.getFile().getRepository();

        for (final FileHistoryNode child : oldNode.getChildren()) {
            // don't copy deleted children
            if (!child.isDeleted()) {
                final String childPath = child.getFile().getPath();
                FileHistoryGraph.this.addCopy(childPath,
                        crateCopyTargetName(childPath, oldParentPath, newParentPath),
                        oldRevision, newRevision, repository);
            }
        }
    }

    /**
     * Returns or creates a {@link FileHistoryNode} for a given {@link FileInRevision}.
     * If a node for that {@link FileInRevision} already exists, it is returned.
     * If a node for that {@link FileInRevision} does not exist, it is created.
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
    private FileHistoryNode getOrCreateFileHistoryNode(final FileInRevision file, final boolean mustNotExist,
            final boolean isNew, final boolean copyChildren) {
        FileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            final FileHistoryNode newNode = new FileHistoryNode(file, false);
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
    public final FileHistoryNode getNodeFor(final FileInRevision file) {
        final Pair<String, Repository> key = this.createKey(file);
        final List<FileHistoryNode> nodesForKey = this.index.get(key);
        for (final FileHistoryNode node : nodesForKey) {
            if (node.getFile().getRevision().equals(file.getRevision())) {
                return node;
            }
        }
        return null;
    }

    private Pair<String, Repository> createKey(final FileInRevision file) {
        return Pair.create(file.getPath(), file.getRepository());
    }

    /**
     * Performs a file lookup in the index.
     * @param file The file to look for.
     * @return A list of matching {@link FileHistoryNode}s.
     */
    protected final List<FileHistoryNode> lookupFile(final FileInRevision file) {
        final Pair<String, Repository> key = this.createKey(file);
        return this.index.get(key);
    }

    @Override
    public final List<FileInRevision> getLatestFiles(final FileInRevision file) {
        Set<FileHistoryNode> nodes = this.getLatestFilesHelper(file, false);
        if (nodes.isEmpty()) {
            nodes = this.getLatestFilesHelper(file, true);
        }

        if (nodes.isEmpty()) {
            return Collections.singletonList(file);
        } else {
            final List<FileInRevision> revs = new ArrayList<>();
            for (final FileHistoryNode node : nodes) {
                revs.add(node.getFile());
            }
            return FileInRevision.sortByRevision(revs);
        }
    }

    /**
     * Returns the latest known nodes of the given file. If the file is unknown, a list with the file itself is
     * returned.
     *
     * @param returnDeletions If <code>true</code> and all versions were deleted, the last known nodes
     *      before deletion are returned. If <code>false</code>, no nodes are returned in this case.
     */
    private Set<FileHistoryNode> getLatestFilesHelper(final FileInRevision file, final boolean returnDeletions) {
        final FileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            // unknown file, return a new node for that file without ancestors or descendants
            return Collections.<FileHistoryNode>singleton(new FileHistoryNode(file, false));
        } else {
            // either node for file or descendant node shares history with passed file, follow it
            return this.getLatestFilesHelper(node, returnDeletions);
        }
    }

    /**
     * Returns the latest known successor nodes of the given node. Branching (e.g. because of copy/rename operations)
     * is handled properly.
     *
     * @param returnDeletions If <code>true</code> and all versions were deleted, the last known nodes
     *      before deletion are returned. If <code>false</code>, no nodes are returned in this case.
     */
    private Set<FileHistoryNode> getLatestFilesHelper(final FileHistoryNode node, final boolean returnDeletions) {
        // deletion nodes are never returned
        if (!node.isDeleted()) {
            if (node.getDescendants().isEmpty()) {
                return Collections.singleton(node);
            } else {
                // is this node the last known one given its path?
                final Set<FileHistoryNode> result = new LinkedHashSet<>();
                boolean samePathFound = false;
                for (final FileHistoryEdge descendantEdge : node.getDescendants()) {
                    final FileHistoryNode descendant = descendantEdge.getDescendant();
                    if (node.getFile().getPath().equals(descendant.getFile().getPath())) {
                        samePathFound = true;
                    }
                    result.addAll(FileHistoryGraph.this.getLatestFilesHelper(descendant, returnDeletions));
                }
                if (!samePathFound || (returnDeletions && result.isEmpty())) {
                    // either this node is the last known one existing for its path, or this node is the last node
                    // before deletion and we have been advised to return such nodes
                    result.add(node);
                }
                return result;
            }
        } else {
            return Collections.<FileHistoryNode> emptySet();
        }
    }

    /**
     * Returns the nearest ancestor for passed {@link FileInRevision} having the same path, or <code>null</code>
     * if no suitable node exists. To be suitable, the ancestor node must be an {@link ExistingFileHistoryNode} as
     * a deleted file cannot be an ancestor.
     */
    public abstract FileHistoryNode findAncestorFor(FileInRevision file);

    @Override
    public String toString() {
        return this.index.toString();
    }
}
