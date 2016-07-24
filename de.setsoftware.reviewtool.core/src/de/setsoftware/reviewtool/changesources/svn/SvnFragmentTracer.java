package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;
import de.setsoftware.reviewtool.model.changestructure.LocalRevision;
import de.setsoftware.reviewtool.model.changestructure.RepoRevision;
import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 * A simple svn based fragment tracer that does not trace position changes and only traces file renames.
 */
public class SvnFragmentTracer implements IFragmentTracer {

    private final SVNClientManager mgr;
    private final Map<FileInRevision, FileInRevision> fileCache = new HashMap<>();
    private RenameDetector detector;

    public SvnFragmentTracer(SVNClientManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public Fragment traceFragment(Fragment fragment) {
        //TODO implementieren
        return new Fragment(
                this.traceFile(fragment.getFile()),
                fragment.getFrom(),
                fragment.getTo(),
                fragment.getContent());
    }

    /**
     * Implementation of {@link ISVNLogEntryHandler} that determines the most
     * recent path of a file.
     */
    private static final class RenameDetector implements ISVNLogEntryHandler {

        private final Map<Pair<Long, String>, String> renames = new HashMap<>();

        @Override
        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
            final Collection<SVNLogEntryPath> changedPaths = logEntry.getChangedPaths().values();
            for (final SVNLogEntryPath p : changedPaths) {
                if (p.getCopyPath() != null && thereIsADeletionFor(p.getCopyPath(), changedPaths)) {
                    this.renames.put(Pair.create(p.getCopyRevision(), p.getCopyPath()), p.getPath());
                }
            }
        }

        private static boolean thereIsADeletionFor(String path, Collection<SVNLogEntryPath> changedPaths) {
            for (final SVNLogEntryPath p : changedPaths) {
                if (p.getType() == SVNLogEntryPath.TYPE_DELETED && path.equals(p.getPath())) {
                    return true;
                }
            }
            return false;
        }

        public String trace(String path) {
            long minRevision = Integer.MAX_VALUE;
            long maxRevision = 0;
            for (final Pair<Long, String> p : this.renames.keySet()) {
                minRevision = Math.min(minRevision, p.getFirst());
                maxRevision = Math.max(maxRevision, p.getFirst());
            }

            String curPath = path;
            for (long revision = minRevision; revision <= maxRevision; revision++) {
                final Pair<Long, String> key = Pair.create(revision, curPath);
                final String newPath = this.renames.get(key);
                if (newPath != null) {
                    curPath = newPath;
                }
            }
            return curPath;
        }

    }

    @Override
    public FileInRevision traceFile(FileInRevision file) {
        if (this.fileCache.containsKey(file)) {
            return this.fileCache.get(file);
        }

        final SvnRepo repo = (SvnRepo) file.getRepository();

        if (this.detector == null) {
            try {
                this.detector = new RenameDetector();
                this.mgr.getLogClient().doLog(
                        repo.getRemoteUrl(),
                        new String[] {"/"},
                        SVNRevision.HEAD,
                        SVNRevision.HEAD,
                        this.mapRevision(file.getRevision()),
                        false,
                        true,
                        false,
                        1000,
                        null,
                        this.detector);
            } catch (final SVNException e) {
                throw new ReviewtoolException(e);
            }
        }

        final FileInRevision ret = new FileInRevision(
                this.detector.trace(file.getPath()), new LocalRevision(), file.getRepository());
        this.fileCache.put(file, ret);
        return ret;
    }

    private SVNRevision mapRevision(Revision revision) {
        //instead of SVNRevision.HEAD, SVNRevision.WORKING would be more correct, but also harder to implement
        return revision instanceof LocalRevision ? SVNRevision.HEAD :
            SVNRevision.create((Long) ((RepoRevision) revision).getId());
    }

}
