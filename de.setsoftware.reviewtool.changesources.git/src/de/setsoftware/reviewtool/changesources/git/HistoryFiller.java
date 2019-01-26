package de.setsoftware.reviewtool.changesources.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;

/**
 * Helper class that populates the file history graphs with the relevant subset of revisions.
 */
class HistoryFiller {

    private final List<GitRevision> logEntries = new ArrayList<>();

    public void register(GitRevision logEntry) {
        this.logEntries .add(logEntry);
    }

    public void populate(List<GitRevision> relevantRevisions) throws IOException {
        final Set<GitRepository> repos = new LinkedHashSet<>();
        long minTime = Long.MAX_VALUE;
        for (final GitRevision r : relevantRevisions) {
            repos.add(r.getWorkingCopy().getRepository());
            minTime = Math.min(minTime, r.getDate().getTime());
        }

        final Multimap<GitRepository, GitRevision> revisionsToAnalyze = new Multimap<>();
        for (final GitRevision r : this.logEntries) {
            final GitRepository repository = r.getWorkingCopy().getRepository();
            if (repos.contains(repository)
                    && r.getDate().getTime() >= minTime) {
                revisionsToAnalyze.put(repository, r);
            }
        }

        for (final GitRepository repo : revisionsToAnalyze.keySet()) {
            final IMutableFileHistoryGraph graph = repo.getFileHistoryGraph();
            for (final GitRevision r : revisionsToAnalyze.get(repo)) {
                r.analyzeRevision(graph);
            }
        }
    }

}
