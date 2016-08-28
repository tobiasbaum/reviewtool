package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.Commit;
import de.setsoftware.reviewtool.model.changestructure.IChangeData;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;
import de.setsoftware.reviewtool.model.changestructure.RepositoryChangeHistory;

/**
 * Implementation of {@link IChangeData} that caches data needed for tracing in addition to the matched commits.
 */
public class SvnChangeData implements IChangeData {

    private final List<Commit> commits;
    private final FileHistoryGraph fileHistory;
    private final RepositoryChangeHistory changeHistory;

    public SvnChangeData(List<Commit> commits, FileHistoryGraph fileHistory) {
        this.commits = commits;
        this.fileHistory = fileHistory;
        this.changeHistory = new RepositoryChangeHistory(commits);
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
    public IFragmentTracer createTracer() {
        return new SvnFragmentTracer(this.createChangeHistory(), this.fileHistory);
    }

    @Override
    public RepositoryChangeHistory createChangeHistory() {
        return this.changeHistory;
    }

}
