package de.setsoftware.reviewtool.changesources.core;

import java.util.HashMap;
import java.util.Map;

import de.setsoftware.reviewtool.base.IPartiallyComparable;

/**
 * Represents a cache for file contents for a single repository.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 * @param <RepoT> Type of the repository.
 */
final class FileCache<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>,
        RepoT extends IScmRepository<ItemT, CommitIdT, CommitT, RepoT>> {

    /**
     * Encapsulates a file whose contents are to be cached.
     *
     * @param <CommitIdT> Type of a commit ID.
     */
    private static final class CachedFile<CommitIdT extends IPartiallyComparable<CommitIdT>> {
        final String path;
        final CommitIdT commitId;

        /**
         * Constructor.
         * @param path The file path.
         * @param commitId The file revision.
         */
        CachedFile(final String path, final CommitIdT commitId) {
            this.path = path;
            this.commitId = commitId;
        }

        @Override
        public boolean equals(final Object object) {
            if (object instanceof CachedFile) {
                @SuppressWarnings("unchecked")
                final CachedFile<CommitIdT> entry = (CachedFile<CommitIdT>) object;
                return this.path.equals(entry.path) && this.commitId.equals(entry.commitId);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.path.hashCode() ^ this.commitId.hashCode();
        }
    }

    private final RepoT repo;
    private final Map<CachedFile<CommitIdT>, byte[]> fileContents;

    /**
     * Constructor.
     * @param repo The underlying {@link IScmRepositoryBridge}.
     */
    FileCache(final RepoT repo) {
        this.repo = repo;
        this.fileContents = new HashMap<>();
    }

    /**
     * Returns the contents of some file in the repository.
     *
     * @param path The file path.
     * @param commitId The file revision.
     * @return The file contents as a byte array.
     */
    byte[] getFileContents(final String path, final CommitIdT commitId) throws ScmException {
        final CachedFile<CommitIdT> entry = new CachedFile<>(path, commitId);
        byte[] contents;
        synchronized (this) {
            contents = this.fileContents.get(entry);
            if (contents == null) {
                contents = this.repo.loadContents(path, commitId);
                this.fileContents.put(entry, contents);
            }
        }
        return contents;
    }
}
