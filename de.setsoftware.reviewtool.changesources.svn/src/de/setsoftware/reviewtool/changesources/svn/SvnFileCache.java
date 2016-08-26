package de.setsoftware.reviewtool.changesources.svn;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/**
 * Represents a cache for file contents pulled from SVN repositories.
 */
public class SvnFileCache {

    /**
     * Encapsulates a file whose contents are to be cached.
     */
    private static final class CachedFile {
        final SvnRepo repoUrl;
        final String path;
        final long revision;

        /**
         * Constructor.
         * @param repoUrl The {@link SvnRepo}.
         * @param path The file path.
         * @param revision The file revision.
         */
        CachedFile(final SvnRepo repoUrl, final String path, long revision) {
            this.repoUrl = repoUrl;
            this.path = path;
            this.revision = revision;
        }

        @Override
        public boolean equals(final Object object) {
            if (object instanceof CachedFile) {
                final CachedFile entry = (CachedFile) object;
                return this.repoUrl.equals(entry.repoUrl)
                        && this.path.equals(entry.path)
                        && this.revision == entry.revision;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.repoUrl.hashCode() ^ this.path.hashCode() ^ Long.valueOf(this.revision).hashCode();
        }
    }

    private final SVNClientManager mgr;
    private final Map<CachedFile, byte[]> fileContents;

    /**
     * Constructor.
     * @param mgr The {@link SVNClientManager} to use.
     */
    public SvnFileCache(final SVNClientManager mgr) {
        this.mgr = mgr;
        this.fileContents = new HashMap<>();
    }

    /**
     * Returns the contents of some file in the repository.
     * @param repoUrl The {@link SvnRepo}.
     * @param path The file path.
     * @param revision The file revision.
     * @return The file contents as a byte array or null if some error occurs.
     */
    public byte[] getFileContents(final SvnRepo repoUrl, final String path, long revision) {
        final CachedFile entry = new CachedFile(repoUrl, path, revision);
        byte[] contents = this.fileContents.get(entry);
        if (contents == null) {
            try {
                contents = this.loadFile(repoUrl, path, revision);
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
    private byte[] loadFile(SvnRepo repoUrl, String path, long revision) throws SVNException {
        final SVNRepository repo = this.mgr.getRepositoryPool().createRepository(repoUrl.getRemoteUrl(), true);
        final ByteArrayOutputStream contents = new ByteArrayOutputStream();
        if (repo.checkPath(path, revision) != SVNNodeKind.FILE) {
            return new byte[0];
        }
        repo.getFile(path, revision, null, contents);
        return contents.toByteArray();
    }
}
