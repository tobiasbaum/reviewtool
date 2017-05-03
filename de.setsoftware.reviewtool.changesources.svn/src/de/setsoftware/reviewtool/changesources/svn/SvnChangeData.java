package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.Commit;
import de.setsoftware.reviewtool.model.changestructure.IChangeData;
import de.setsoftware.reviewtool.model.changestructure.IChangeSource;
import de.setsoftware.reviewtool.model.changestructure.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.Repository;

/**
 * Implementation of {@link IChangeData} that caches data needed for tracing in addition to the matched commits.
 */
public class SvnChangeData implements IChangeData {

    private final IChangeSource source;
    private final Collection<? extends Repository> repositories;
    private final List<Commit> commits;
    private final List<File> localPaths;
    private final IFileHistoryGraph fileHistoryGraph;

    public SvnChangeData(
            final IChangeSource source,
            final Collection<? extends Repository> repositories,
            final List<Commit> commits,
            final List<File> localPaths,
            final IFileHistoryGraph fileHistoryGraph) {
        this.source = source;
        this.repositories = repositories;
        this.commits = commits;
        this.localPaths = localPaths;
        this.fileHistoryGraph = fileHistoryGraph;
    }

    @Override
    public IChangeSource getSource() {
        return this.source;
    }

    @Override
    public Collection<? extends Repository> getRepositories() {
        return Collections.unmodifiableCollection(this.repositories);
    }

    @Override
    public List<Commit> getMatchedCommits() {
        final List<Commit> ret = new ArrayList<>();
        for (final Commit c : this.commits) {
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
