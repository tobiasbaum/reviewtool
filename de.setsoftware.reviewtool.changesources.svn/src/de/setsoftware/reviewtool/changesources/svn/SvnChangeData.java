package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;

/**
 * Implementation of {@link IChangeData} that caches data needed for tracing in addition to the matched commits.
 */
public class SvnChangeData implements IChangeData {

    private final IChangeSource source;
    private final List<ICommit> commits;
    private final List<File> localPaths;
    private final IFileHistoryGraph fileHistoryGraph;

    public SvnChangeData(
            final IChangeSource source,
            final List<ICommit> commits,
            final List<File> localPaths,
            final IFileHistoryGraph fileHistoryGraph) {
        this.source = source;
        this.commits = commits;
        this.localPaths = localPaths;
        this.fileHistoryGraph = fileHistoryGraph;
    }

    @Override
    public IChangeSource getSource() {
        return this.source;
    }

    @Override
    public List<? extends ICommit> getMatchedCommits() {
        final List<ICommit> ret = new ArrayList<>();
        for (final ICommit c : this.commits) {
            if (c.isVisible()) {
                ret.add(c);
            }
        }
        return ret;
    }

    @Override
    public List<File> getLocalPaths() {
        return this.localPaths;
    }

    @Override
    public IFileHistoryGraph getHistoryGraph() {
        return this.fileHistoryGraph;
    }

}
