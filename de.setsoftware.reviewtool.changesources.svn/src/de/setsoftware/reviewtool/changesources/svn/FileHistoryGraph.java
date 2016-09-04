package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.RepoRevision;
import de.setsoftware.reviewtool.model.changestructure.Repository;
import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 *  A graph of files. Tracks renames, copies and deletion, so that the history of a file forms a tree.
 */
class FileHistoryGraph {

    /**
     * A node in one of the history trees.
     * When it has no targets, it is a pure deletion.
     */
    private static class FileHistoryNode {

        private final FileInRevision source;
        private final List<FileInRevision> targets;

        public FileHistoryNode(FileInRevision source) {
            this.source = source;
            this.targets = new ArrayList<>();
        }

        public FileInRevision getSource() {
            return this.source;
        }

        public List<FileInRevision> getTargets() {
            return this.targets;
        }

        public void addTarget(FileInRevision fileTo) {
            this.targets.add(fileTo);
        }

        public void makeDeletion() {
            final Iterator<FileInRevision> iter = this.targets.iterator();
            while (iter.hasNext()) {
                final FileInRevision target = iter.next();
                if (target.getPath().equals(this.source.getPath())) {
                    iter.remove();
                }
            }
        }
    }

    private final Multimap<Pair<String, Repository>, FileHistoryNode> index = new Multimap<>();

    /**
     * Adds the information that the file with the given path was deleted with the commit of the given revision.
     */
    public void addDeletion(String path, Revision revision, Repository repo) {
        final FileInRevision file = ChangestructureFactory.createFileInRevision(path, adjust(revision, -1), repo);
        final FileHistoryNode node = this.getNodeForExactRevision(file);
        if (node == null) {
            this.index.put(this.createKey(file), new FileHistoryNode(file));
        } else {
            node.makeDeletion();
        }
    }

    /**
     * Adds the information that the file with the given "from" path was copied with the commit of the given revision
     * to the given "to" path.
     */
    public void addCopy(
            String pathFrom, String pathTo, Revision revisionFrom, Revision revisionTo, Repository repo) {
        final FileInRevision fileFrom = ChangestructureFactory.createFileInRevision(pathFrom, revisionFrom, repo);
        final FileInRevision fileTo = ChangestructureFactory.createFileInRevision(pathTo, revisionTo, repo);
        FileHistoryNode node = this.getNodeForExactRevision(fileFrom);
        if (node == null) {
            node = new FileHistoryNode(fileFrom);
            node.addTarget(ChangestructureFactory.createFileInRevision(pathFrom, adjust(revisionFrom, 1), repo));
            this.index.put(this.createKey(fileFrom), node);
        }
        node.addTarget(fileTo);
    }

    private FileHistoryNode getNodeForExactRevision(FileInRevision file) {
        final Pair<String, Repository> key = this.createKey(file);
        final List<FileHistoryNode> nodesForKey = this.index.get(key);
        for (final FileHistoryNode node : nodesForKey) {
            if (node.getSource().getRevision().equals(file.getRevision())) {
                return node;
            }
        }
        return null;
    }

    private Pair<String, Repository> createKey(FileInRevision file) {
        return Pair.create(file.getPath(), file.getRepository());
    }

    /**
     * Returns the latest known versions of the given file. If all versions were deleted, the last known versions
     * before deletion are returned. If the file is unknown, a list with the file itself is
     * returned.
     */
    public List<FileInRevision> getLatestFiles(FileInRevision file) {
        final List<FileInRevision> withoutDeletions = this.getLatestFilesHelper(file, false);
        if (withoutDeletions.isEmpty()) {
            return this.getLatestFilesHelper(file, true);
        } else {
            return withoutDeletions;
        }
    }

    private List<FileInRevision> getLatestFilesHelper(FileInRevision file, boolean returnDeletions) {
        final FileHistoryNode node = this.getNodeFor(file);
        if (node == null) {
            return Collections.singletonList(file);
        } else if (node.getTargets().isEmpty()) {
            if (returnDeletions) {
                return Collections.singletonList(node.getSource());
            } else {
                return Collections.emptyList();
            }
        } else {
            final List<FileInRevision> ret = new ArrayList<>();
            for (final FileInRevision target : node.getTargets()) {
                ret.addAll(this.getLatestFilesHelper(target, returnDeletions));
            }
            return ret;
        }
    }

    private FileHistoryNode getNodeFor(FileInRevision file) {
        final Pair<String, Repository> key = this.createKey(file);
        final List<FileHistoryNode> nodesForKey = this.index.get(key);
        final long targetRevision = this.getRevision(file);
        long nearestRevision = Long.MAX_VALUE;
        FileHistoryNode nearestNode = null;
        for (final FileHistoryNode node : nodesForKey) {
            final long nodeRevision = this.getRevision(node.source);
            if (nodeRevision >= targetRevision && nodeRevision < nearestRevision) {
                nearestNode = node;
                nearestRevision = nodeRevision;
            }
        }
        return nearestNode;
    }

    private long getRevision(FileInRevision source) {
        final Revision rev = source.getRevision();
        return rev instanceof RepoRevision ? (Long) ((RepoRevision) rev).getId() : Long.MAX_VALUE;
    }

    private static Revision adjust(Revision revision, int change) {
        final long oldRevision = (Long) ((RepoRevision) revision).getId();
        return ChangestructureFactory.createRepoRevision(oldRevision + change);
    }

}
