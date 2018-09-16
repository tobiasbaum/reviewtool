package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;

/**
 * A stub implementation of {@link AbstractRepository} for use by tests.
 */
public final class StubRepo extends AbstractRepository implements ISvnRepo {

    public static StubRepo INSTANCE = new StubRepo("");
    private static final long serialVersionUID = 1L;

    private IMutableFileHistoryGraph fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    private final String relPath;

    public StubRepo(final String relPath) {
        this.relPath = relPath;
    }

    @Override
    public String getId() {
        return "stub";
    }

    @Override
    public IRepoRevision<?> toRevision(final String revisionId) {
        return ChangestructureFactory.createRepoRevision(ComparableWrapper.wrap(revisionId), this);
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision<?> revision) {
        return new byte[0];
    }

    @Override
    public IMutableFileHistoryGraph getFileHistoryGraph() {
        return this.fileHistoryGraph;
    }

    @Override
    public SVNURL getRemoteUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRelativePath() {
        return this.relPath;
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
    public void setFileHistoryGraph(final IMutableFileHistoryGraph fileHistoryGraph) {
        this.fileHistoryGraph = fileHistoryGraph;
    }

    @Override
    public Set<? extends File> getFiles(final String path, final IRepoRevision<?> revision) {
        return Collections.emptySet();
    }
}
