package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.Commit;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.IChangeData;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;

/**
 * Implementation of {@link IChangeData} that caches data needed for tracing in addition to the matched commits.
 */
public class SvnChangeData implements IChangeData {

    private final List<Commit> commits;
    private final SvnFileHistoryGraph fileHistoryGraph;

    public SvnChangeData(List<Commit> commits, final SvnFileHistoryGraph fileHistoryGraph) {
        this.commits = commits;
        this.fileHistoryGraph = fileHistoryGraph;
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
        return new SvnFragmentTracer(this.fileHistoryGraph);
    }

    @Override
    public FileHistoryGraph getHistoryGraph() {
        return this.fileHistoryGraph;
    }

}
