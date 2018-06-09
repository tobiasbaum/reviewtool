package de.setsoftware.reviewtool.changesources.svn;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * Represents a cache for file contents for a single SVN repository.
 */
final class SvnFileCache {

    /**
     * Encapsulates a file whose contents are to be cached.
     */
    private static final class CachedFile {
        final String path;
        final long revision;

        /**
         * Constructor.
         * @param path The file path.
         * @param revision The file revision.
         */
        CachedFile(final String path, final long revision) {
            this.path = path;
            this.revision = revision;
        }

        @Override
        public boolean equals(final Object object) {
            if (object instanceof CachedFile) {
                final CachedFile entry = (CachedFile) object;
                return this.path.equals(entry.path) && this.revision == entry.revision;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.path.hashCode() ^ Long.valueOf(this.revision).hashCode();
        }
    }

    private final SVNRepository repo;
    private final Map<CachedFile, byte[]> fileContents;

    /**
     * Constructor.
     * @param repo The {@link SVNRepository}.
     */
    SvnFileCache(final SVNRepository repo) {
        this.repo = repo;
        this.fileContents = new HashMap<>();
    }

    /**
     * Returns the contents of some file in the repository.
     * @param path The file path.
     * @param revision The file revision.
     * @return The file contents as a byte array.
     * @throws SVNException if some error occurs.
     */
    byte[] getFileContents(final String path, final long revision) throws SVNException {
        final CachedFile entry = new CachedFile(path, revision);
        byte[] contents = this.fileContents.get(entry);
        if (contents == null) {
            contents = this.loadFile(path, revision);
            this.fileContents.put(entry, contents);
        }
        return contents;
    }

    /**
     * Loads the contents of some file in the repository.
     * @param path The file path.
     * @param revision The file revision.
     * @return The file contents as a byte array.
     * @throws SVNException if some error occurs.
     */
    private byte[] loadFile(final String path, final long revision) throws SVNException {
        final ByteArrayOutputStream contents = new ByteArrayOutputStream();
        if (this.repo.checkPath(path, revision) != SVNNodeKind.FILE) {
            return new byte[0];
        }
        this.repo.getFile(path, revision, null, contents);
        return contents.toByteArray();
    }
}
