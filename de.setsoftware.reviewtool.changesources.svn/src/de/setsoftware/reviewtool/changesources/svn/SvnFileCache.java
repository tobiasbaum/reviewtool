package de.setsoftware.reviewtool.changesources.svn;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/**
 * Represents a cache for file contents for a single SVN repository.
 */
public class SvnFileCache {

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
        CachedFile(final String path, long revision) {
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

    private final SVNClientManager mgr;
    private final SvnRepo repoUrl;
    private final Map<CachedFile, byte[]> fileContents;

    /**
     * Constructor.
     * @param mgr The {@link SVNClientManager} to use.
     * @param repoUrl The {@link SvnRepo}.
     */
    public SvnFileCache(final SVNClientManager mgr, final SvnRepo repoUrl) {
        this.mgr = mgr;
        this.repoUrl = repoUrl;
        this.fileContents = new HashMap<>();
    }

    /**
     * Returns the contents of some file in the repository.
     * @param path The file path.
     * @param revision The file revision.
     * @return The file contents as a byte array or null if some error occurs.
     */
    public byte[] getFileContents(final String path, final long revision) {
        final CachedFile entry = new CachedFile(path, revision);
        byte[] contents = this.fileContents.get(entry);
        if (contents == null) {
            try {
                contents = this.loadFile(this.repoUrl, path, revision);
                this.fileContents.put(entry, contents);
            } catch (final SVNException e) {
                return null;
            }
        }
        return contents;
    }

    /**
     * Loads the contents of some file in the repository.
     * @param repoUrl The {@link SvnRepo}.
     * @param path The file path.
     * @param revision The file revision.
     * @return The file contents as a byte array.
     * @throws SVNException if some error occurs.
     */
    private byte[] loadFile(final SvnRepo repoUrl, final String path, final long revision) throws SVNException {
        final SVNRepository repo = this.mgr.getRepositoryPool().createRepository(repoUrl.getRemoteUrl(), true);
        final ByteArrayOutputStream contents = new ByteArrayOutputStream();
        if (repo.checkPath(path, revision) != SVNNodeKind.FILE) {
            return new byte[0];
        }
        repo.getFile(path, revision, null, contents);
        return contents.toByteArray();
    }
}
