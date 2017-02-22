package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ValueWrapper;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileDiff;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryEdge;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryNode;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.IncompatibleFragmentException;
import de.setsoftware.reviewtool.model.changestructure.RepoRevision;
import de.setsoftware.reviewtool.model.changestructure.Repository;
import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
final class SvnFileHistoryGraph implements FileHistoryGraph {

    /**
     * An edge in the history tree.
     */
    public static final class SvnFileHistoryEdge implements FileHistoryEdge {

        private final SvnFileHistoryNode target;
        private FileDiff diff;

        public SvnFileHistoryEdge(final SvnFileHistoryNode target, final FileDiff diff) {
            this.target = target;
            this.diff = diff;
        }

        @Override
        public SvnFileHistoryNode getTarget() {
            return this.target;
        }

        @Override
        public FileDiff getDiff() {
            return this.diff;
        }

        void setDiff(final FileDiff diff) {
            this.diff = diff;
        }

    }

    /**
     * A node in one of the history trees.
     * It is bound to a {@link FileInRevision} and knows at most one direct ancestor, called its source.
     */
    public abstract static class SvnFileHistoryNode implements FileHistoryNode {

        private final FileInRevision file;
        private SvnFileHistoryEdge ancestor;
        private final List<SvnFileHistoryNode> children;
        private static final ThreadLocal<Boolean> inToString = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };

        /**
         * Creates a {@link SvnFileHistoryNode}. The source is initially set to <code>null</code>.
         * @param file The {@link FileInRevision} to wrap.
         */
        public SvnFileHistoryNode(final FileInRevision file) {
            this.file = file;
            this.children = new ArrayList<>();
        }

        @Override
        public FileInRevision getFile() {
            return this.file;
        }

        @Override
        public boolean isRoot() {
            return this.ancestor == null;
        }

        @Override
        public SvnFileHistoryEdge getAncestor() {
            return this.ancestor;
        }

        @Override
        public FileDiff buildHistory(final FileHistoryNode from) {
            if (from.equals(this)) {
                return new FileDiff();
            }

            if (this.ancestor == null) {
                return null;
            } else {
                final FileDiff diff = this.ancestor.getTarget().buildHistory(from);
                if (diff != null) {
                    try {
                        return diff.merge(this.ancestor.getDiff());
                    } catch (final IncompatibleFragmentException e) {
                        throw new Error(e);
                    }
                }
            }

            return null; // ancestor revision not found
        }

        /**
         * Sets the nearest ancestor {@link SvnFileHistoryNode}.
         * This operation is called internally when this node becomes/stops being a target of some other
         * {@link SvnFileHistoryNode}.
         */
        void setSource(final SvnFileHistoryEdge ancestor) {
            this.ancestor = ancestor;
        }

        /**
         * Returns a list of child {@link SvnFileHistoryNode}s.
         */
        public List<SvnFileHistoryNode> getChildren() {
            return this.children;
        }

        /**
         * Adds a child {@link SvnFileHistoryNode}.
         */
        public void addChild(final SvnFileHistoryNode child) {
            this.children.add(child);
        }

        /**
         * Accepts a {@link FileHistoryNodeVisitor}.
         */
        public abstract void accept(FileHistoryNodeVisitor v);

        /**
         * {@inheritDoc}
         * <p/>
         * In order to prevent infinite recursion, a thread-local flag <code>inToString</code> is used to detect cycles.
         */
        @Override
        public String toString() {
            if (inToString.get()) {
                return this.file.toString();
            }

            inToString.set(true);
            try {
                final List<String> attributes = new ArrayList<>();
                this.attributesToString(attributes);
                if (attributes.isEmpty()) {
                    return this.file.toString();
                } else {
                    return this.file.toString() + attributes.toString();
                }
            } finally {
                inToString.set(false);
            }
        }

        /**
         * Fills a list of additional attributes used by toString().
         * @param attributes A list containing elements to be included in the output of {@link #toString()}.
         */
        protected void attributesToString(final List<String> attributes) {
            if (this.ancestor != null) {
                attributes.add("ancestor=" + this.ancestor.getTarget());
            }
            if (!this.children.isEmpty()) {
                attributes.add("children=" + this.children);
            }
        }

    }

    /**
     * A node in one of the history trees.
     * It denotes an existing revisioned file in the {@link SvnFileHistoryGraph}.
     * It has a list of target {@link SvnFileHistoryNode}s this file evolves to due to changes, copies, or deletions.
     */
    public static final class ExistingFileHistoryNode extends SvnFileHistoryNode {

        private final Set<SvnFileHistoryNode> targets;

        public ExistingFileHistoryNode(final FileInRevision file) {
            super(file);
            this.targets = new LinkedHashSet<>();
        }

        @Override
        protected final void attributesToString(final List<String> attributes) {
            super.attributesToString(attributes);
            if (!this.targets.isEmpty()) {
                attributes.add("targets=" + this.targets);
            }
        }

        @Override
        public Set<SvnFileHistoryNode> getTargets() {
            return this.targets;
        }

        /**
         * Adds a target {@link SvnFileHistoryNode}s this node evolves to.
         */
        public void addTarget(final SvnFileHistoryNode target, final FileDiff diff) {
            this.targets.add(target);
            target.setSource(new SvnFileHistoryEdge(this, diff));
        }

        @Override
        public final void accept(FileHistoryNodeVisitor v) {
            v.handleExistingNode(this);
        }
    }

    /**
     * A node in one of the history trees.
     * When it has no targets, it is a pure deletion.
     */
    public static final class NonExistingFileHistoryNode extends SvnFileHistoryNode {

        public NonExistingFileHistoryNode(final FileInRevision file) {
            super(file);
        }

        @Override
        public Set<FileHistoryNode> getTargets() {
            return Collections.emptySet();
        }

        @Override
        public final void accept(FileHistoryNodeVisitor v) {
            v.handleNonExistingNode(this);
        }
    }

    /**
     * Visits a {@link SvnFileHistoryNode}.
     */
    public static interface FileHistoryNodeVisitor {

        /**
         * Handles an {@link ExistingFileHistoryNode}.
         */
        public abstract void handleExistingNode(ExistingFileHistoryNode node);

        /**
         * Handles a {@link NonExistingFileHistoryNode}.
         */
        public abstract void handleNonExistingNode(NonExistingFileHistoryNode node);
    }

    private final Multimap<Pair<String, Repository>, SvnFileHistoryNode> index = new Multimap<>();

    /**
     * Returns true if passed path is known to this {@link FileHistoryGraph}.
     * @param path The path to check.
     * @param repo The repository.
     * @return <code>true</code> if the path is known, else <code>false</code>
     */
    boolean contains(final String path, final Repository repo) {
        return !this.index.get(Pair.create(path, repo)).isEmpty();
    }

    /**
     * Adds the information that the file with the given path was changed at the commit of the given revision.
     */
    void addChange(String path, Revision prevRevision, Revision revision, Repository repo) {
        final FileInRevision file = ChangestructureFactory.createFileInRevision(path, revision, repo);
        final SvnFileHistoryNode node = this.getOrCreateExistingFileHistoryNode(file, true);
        if (node.isRoot()) {
            // if a file starts its existence within the history graph with a change, we need an
            // artificial ancestor node to record the changes
            final FileInRevision prevFile = ChangestructureFactory.createFileInRevision(path, prevRevision, repo);
            final ExistingFileHistoryNode ancestor = this.getOrCreateExistingFileHistoryNode(prevFile, true);
            ancestor.addTarget(node, new FileDiff());
        }
    }

    /**
     * Adds the information that the file with the given path was deleted with the commit of the given revision.
     * If an ancestor node exists, the deletion node of type {@link NonExistingFileHistoryNode} is linked to it,
     * possibly creating an intermediate {@link ExistingFileHistoryNode} just before the deletion. This supports
     * finding the last revision of a file before being deleted.
     */
    void addDeletion(String path, Revision prevRevision, Revision revision, Repository repo) {
        final FileInRevision previousFile =
                ChangestructureFactory.createFileInRevision(path, prevRevision, repo);
        final ExistingFileHistoryNode oldNode = this.getOrCreateExistingFileHistoryNode(previousFile, false);

        final FileInRevision file = ChangestructureFactory.createFileInRevision(path, revision, repo);
        // a file can have at most one associated node per revision
        assert this.getNodeFor(file) == null;

        final SvnFileHistoryNode deletionNode = new NonExistingFileHistoryNode(file);
        this.addParentNodes(deletionNode);
        final Pair<String, Repository> key = this.createKey(file);
        this.index.put(key, deletionNode);
        oldNode.addTarget(deletionNode, new FileDiff());

        for (final SvnFileHistoryNode child : oldNode.getChildren()) {
            this.addDeletion(child.getFile().getPath(), prevRevision, revision, repo);
        }
    }

    /**
     * Adds the information that the file with the given "from" path was copied with the commit of the given revision
     * to the given "to" path.
     */
    void addCopy(
            final String pathFrom, final String pathTo, final Revision revisionFrom, final Revision revisionTo,
            final Repository repo) {
        final FileInRevision fileFrom = ChangestructureFactory.createFileInRevision(pathFrom, revisionFrom, repo);
        final FileInRevision fileTo = ChangestructureFactory.createFileInRevision(pathTo, revisionTo, repo);

        final ExistingFileHistoryNode fromNode = this.getOrCreateExistingFileHistoryNode(fileFrom, false);
        final ExistingFileHistoryNode toNode = this.getOrCreateExistingFileHistoryNode(fileTo, true);

        if (!fromNode.getTargets().contains(toNode)) {
            fromNode.addTarget(toNode, new FileDiff());
            this.copyChildNodes(fromNode, toNode);
        }
    }

    /**
     * Adds a parent node to the node passed. Either an existing parent node is used or a new one is created.
     * @param node The node which to find/create parent node for.
     */
    private void addParentNodes(final SvnFileHistoryNode node) {
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
                final SvnFileHistoryNode parent = this.getOrCreateFileHistoryNode(fileRev, false, false);
                parent.addChild(node);
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
    private void copyChildNodes(final SvnFileHistoryNode oldNode, final SvnFileHistoryNode newNode) {
        final String oldParentPath = oldNode.getFile().getPath();
        final Revision oldRevision = oldNode.getFile().getRevision();
        final String newParentPath = newNode.getFile().getPath();
        final Revision newRevision = newNode.getFile().getRevision();
        final Repository repository = oldNode.getFile().getRepository();

        for (final SvnFileHistoryNode child : oldNode.getChildren()) {
            child.accept(new FileHistoryNodeVisitor() {
                @Override
                public void handleExistingNode(ExistingFileHistoryNode node) {
                    final String childPath = child.getFile().getPath();
                    SvnFileHistoryGraph.this.addCopy(childPath,
                            crateCopyTargetName(childPath, oldParentPath, newParentPath),
                            oldRevision, newRevision, repository);
                }

                @Override
                public void handleNonExistingNode(NonExistingFileHistoryNode node) {
                    // don't copy deleted children
                }
            });
        }
    }

    /**
     * Returns or creates an {@link ExistingFileHistoryNode} for a given {@link FileInRevision}.
     * If a node for that {@link FileInRevision} already exists, it must be of type {@link ExistingFileHistoryNode}.
     * If a node for that {@link FileInRevision} does not exist, it is created. In addition, it is inserted into a
     * possibly existing ancestor/descendant chain and/or parent/child of other {@link SvnFileHistoryNode}s.
     * Finally, child nodes are copied from the ancestor node.
     *
     * @param mustNotExist If <code>true</code>, the node may not already exist.
     */
    private ExistingFileHistoryNode getOrCreateExistingFileHistoryNode(final FileInRevision file,
            final boolean mustNotExist) {
        final SvnFileHistoryNode node = this.getOrCreateFileHistoryNode(file, mustNotExist, true);
        return (ExistingFileHistoryNode) node;
    }

    /**
     * Returns or creates an {@link ExistingFileHistoryNode} for a given {@link FileInRevision}.
     * If a node for that {@link FileInRevision} already exists, it is returned.
     * If a node for that {@link FileInRevision} does not exist, it is created as a {@link ExistingFileHistoryNode}.
     * In addition, it is inserted into a possibly existing ancestor/descendant chain and/or parent/child of other
     * {@link SvnFileHistoryNode}s.
     *
     * @param mustNotExist If <code>true</code>, the node may not already exist.
     * @param copyChildren If <code>true</code> and a new node is created, child nodes are copied from the ancestor
     *      (if it exists) to the new node.
     */
    private SvnFileHistoryNode getOrCreateFileHistoryNode(final FileInRevision file, final boolean mustNotExist,
            final boolean copyChildren) {
        SvnFileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            final ExistingFileHistoryNode newNode = new ExistingFileHistoryNode(file);
            this.index.put(this.createKey(file), newNode);

            final ExistingFileHistoryNode ancestor = this.findAncestorFor(file);
            if (ancestor != null) {
                this.injectInteriorNode(ancestor, newNode);
                ancestor.addTarget(newNode, new FileDiff());
                if (copyChildren) {
                    this.copyChildNodes(ancestor, newNode);
                }
            }

            this.addParentNodes(newNode);
            return newNode;
        } else {
            assert !mustNotExist;
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
    private void injectInteriorNode(final ExistingFileHistoryNode ancestor, final ExistingFileHistoryNode descendant) {
        final Iterator<SvnFileHistoryNode> it = ancestor.getTargets().iterator();
        while (it.hasNext()) {
            final SvnFileHistoryNode target = it.next();
            // only inject interior node if it's the same path (i.e. no rename/move)
            if (ancestor.getFile().getPath().equals(target.getFile().getPath())) {
                it.remove();
                descendant.addTarget(target, target.getAncestor().getDiff());
            }
        }
    }

    @Override
    public SvnFileHistoryNode getNodeFor(FileInRevision file) {
        final Pair<String, Repository> key = this.createKey(file);
        final List<SvnFileHistoryNode> nodesForKey = this.index.get(key);
        for (final SvnFileHistoryNode node : nodesForKey) {
            if (node.getFile().getRevision().equals(file.getRevision())) {
                return node;
            }
        }
        return null;
    }

    private Pair<String, Repository> createKey(FileInRevision file) {
        return Pair.create(file.getPath(), file.getRepository());
    }

    @Override
    public Set<FileInRevision> getLatestFiles(FileInRevision file) {
        Set<SvnFileHistoryNode> nodes = this.getLatestFilesHelper(file, false);
        if (nodes.isEmpty()) {
            nodes = this.getLatestFilesHelper(file, true);
        }

        if (nodes.isEmpty()) {
            return Collections.singleton(file);
        } else {
            final Set<FileInRevision> revs = new HashSet<>();
            for (final SvnFileHistoryNode node : nodes) {
                revs.add(node.getFile());
            }
            return revs;
        }
    }

    /**
     * Returns the latest known nodes of the given file. If the file is unknown, a list with the file itself is
     * returned.
     *
     * @param returnDeletions If <code>true</code> and all versions were deleted, the last known nodes
     *      before deletion are returned. If <code>false</code>, no nodes are returned in this case.
     */
    private Set<SvnFileHistoryNode> getLatestFilesHelper(final FileInRevision file, final boolean returnDeletions) {
        final SvnFileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            // unknown file, return a new node for that file without ancestors or descendants
            return Collections.<SvnFileHistoryNode>singleton(new ExistingFileHistoryNode(file));
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
    private Set<SvnFileHistoryNode> getLatestFilesHelper(final SvnFileHistoryNode node, final boolean returnDeletions) {
        final ValueWrapper<Set<SvnFileHistoryNode>> ret =
                new ValueWrapper<Set<SvnFileHistoryNode>>(new HashSet<SvnFileHistoryNode>());

        node.accept(new FileHistoryNodeVisitor() {

            @Override
            public void handleExistingNode(ExistingFileHistoryNode node) {
                if (node.getTargets().isEmpty()) {
                    ret.get().add(node);
                } else {
                    // is this node the last known one given its path?
                    boolean samePathFound = false;
                    for (final SvnFileHistoryNode target : node.getTargets()) {
                        if (node.getFile().getPath().equals(target.getFile().getPath())) {
                            samePathFound = true;
                        }
                        ret.get().addAll(SvnFileHistoryGraph.this.getLatestFilesHelper(target, returnDeletions));
                    }
                    if (!samePathFound || (returnDeletions && ret.get().isEmpty())) {
                        // either this node is the last known one existing for its path, or this node is the last node
                        // before deletion and we have been advised to return such nodes
                        ret.get().add(node);
                    }
                }
            }

            @Override
            public void handleNonExistingNode(NonExistingFileHistoryNode node) {
                // deletion nodes are never returned
            }

        });
        return ret.get();
    }

    /**
     * Returns the nearest ancestor for passed {@link FileInRevision} having the same path, or <code>null</code>
     * if no suitable node exists. To be suitable, the ancestor node must be an {@link ExistingFileHistoryNode} as
     * a deleted file cannot be an ancestor.
     */
    private ExistingFileHistoryNode findAncestorFor(FileInRevision file) {
        final Pair<String, Repository> key = this.createKey(file);
        final List<SvnFileHistoryNode> nodesForKey = this.index.get(key);
        final long targetRevision = this.getRevision(file);
        long nearestRevision = Long.MIN_VALUE;
        SvnFileHistoryNode nearestNode = null;
        for (final SvnFileHistoryNode node : nodesForKey) {
            final long nodeRevision = this.getRevision(node.file);
            if (nodeRevision < targetRevision && nodeRevision > nearestRevision) {
                nearestNode = node;
                nearestRevision = nodeRevision;
            }
        }
        if (nearestNode != null) {
            return toExistingNodeIfPossible(nearestNode);
        } else {
            return null;
        }
    }

    /**
     * Casts a {@link SvnFileHistoryNode} conditionally to an {@link ExistingFileHistoryNode}.
     * If the cast is no possible, <code>null</code> is returned.
     */
    private static ExistingFileHistoryNode toExistingNodeIfPossible(final SvnFileHistoryNode node) {
        final ValueWrapper<ExistingFileHistoryNode> result = new ValueWrapper<>();
        node.accept(new FileHistoryNodeVisitor() {

            @Override
            public void handleExistingNode(ExistingFileHistoryNode node) {
                result.setValue(node);
            }

            @Override
            public void handleNonExistingNode(NonExistingFileHistoryNode node) {
            }

        });
        return result.get();
    }

    /**
     * Returns the underlying revision number.
     *
     * @param source The revision.
     * @return The revision number.
     */
    private long getRevision(FileInRevision source) {
        final Revision rev = source.getRevision();
        return rev instanceof RepoRevision ? (Long) ((RepoRevision) rev).getId() : Long.MAX_VALUE;
    }

    @Override
    public String toString() {
        return this.index.toString();
    }
}
