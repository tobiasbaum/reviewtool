package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * A stub implementation of {@link AbstractRepository} for use by tests.
 */
public final class StubRepo extends AbstractRepository implements ISvnRepo {

    public static StubRepo INSTANCE = new StubRepo();
    private static final long serialVersionUID = 1L;

    private SvnFileHistoryGraph fileHistoryGraph = new SvnFileHistoryGraph();

    @Override
    public String getId() {
        return "stub";
    }

    @Override
    public IRepoRevision<?> toRevision(final String revisionId) {
        return ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(revisionId), this);
    }

    @Override
    public IRevision getSmallestRevision(final Collection<? extends IRevision> revisions) {
        return getSmallestOfComparableRevisions(revisions);
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision<?> revision) {
        return new byte[0];
    }

    @Override
    public SvnFileHistoryGraph getFileHistoryGraph() {
        return this.fileHistoryGraph;
    }

    @Override
    public SVNURL getRemoteUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CachedLogEntry> getEntries() {
        return Collections.emptyList();
    }

    @Override
    public void appendNewEntries(final Collection<CachedLogEntry> newEntries) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IPath getCacheFilePath() {
        return new Path("");
    }

    @Override
    public void getLog(final long startRevision, final ISVNLogEntryHandler handler) throws SVNException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLatestRevision() throws SVNException {
        return 0;
    }

    @Override
    public void setFileHistoryGraph(final SvnFileHistoryGraph fileHistoryGraph) {
        this.fileHistoryGraph = fileHistoryGraph;
    }
}
