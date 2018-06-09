package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.OperationCanceledException;
import org.tmatesoft.svn.core.SVNException;

import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Handler that filters log entries with a given pattern.
 * Also determines entries that are relevant to restore the full history of files in matching log entries.
 * These revisions have to be considered for a consistent history as otherwise consolidation of diffs is
 * inaccurate. Revisions that are retrofitted are marked as "invisible" in order to be able to differentiate between
 * "proper" and "technically necessary" revisions.
 */
class RelevantRevisionLookupHandler implements CachedLogLookupHandler {

    private final Pattern pattern;
    private final List<SvnRevision> potentiallyRelevantEntries = new ArrayList<>();
    private final List<SvnRevision> entriesSinceLastMatching = new ArrayList<>();
    private SvnRepo currentRoot;

    public RelevantRevisionLookupHandler(final Pattern patternForKey) {
        this.pattern = patternForKey;
    }

    @Override
    public void startNewRepo(final SvnRepo repo) {
        this.currentRoot = repo;
        this.entriesSinceLastMatching.clear();
    }

    @Override
    public void handleLogEntry(final CachedLogEntry logEntry) throws SVNException {
        if (logEntry.getMessage() != null && this.pattern.matcher(logEntry.getMessage()).matches()) {
            assert this.currentRoot != null;
            this.potentiallyRelevantEntries.add(new SvnRevision(this.currentRoot, logEntry, true));
            this.potentiallyRelevantEntries.addAll(this.entriesSinceLastMatching);
            this.entriesSinceLastMatching.clear();
        } else {
            this.entriesSinceLastMatching.add(new SvnRevision(this.currentRoot, logEntry, false));
        }
    }

    /**
     * Returns all revisions that matched the given pattern and all revisions in between that touched
     * files changed in a matching revision.
     */
    public List<ISvnRevision> determineRelevantRevisions(final IMutableFileHistoryGraph historyGraph,
            final IChangeSourceUi ui) {
        final List<ISvnRevision> ret = new ArrayList<>();
        for (final TreeMap<Long, SvnRevision> revisionsInRepo : this.groupResultsByRepository().values()) {
            for (final SvnRevision revision : revisionsInRepo.values()) {
                if (ui.isCanceled()) {
                    throw new OperationCanceledException();
                }
                processRevision(revision, historyGraph);
                if (revision.isVisible()) {
                    ret.add(revision);
                }
            }
        }
        return ret;
    }

    /**
     * Processes a single repository revision by translating it into a file history graph operation.
     *
     * @param revision The revision to process,
     * @param historyGraph The file history graph to operate on.
     */
    public static void processRevision(final ISvnRevision revision, final IMutableFileHistoryGraph historyGraph) {
        for (final Entry<String, CachedLogEntryPath> e : revision.getChangedPaths().entrySet()) {
            final String path = e.getKey();

            final CachedLogEntryPath pathInfo = e.getValue();
            final String copyPath = pathInfo.getCopyPath();
            if (pathInfo.isDeleted()) {
                historyGraph.addDeletion(path, revision.toRevision());
            } else if (pathInfo.isReplaced()) {
                if (copyPath != null) {
                    historyGraph.addReplacement(
                            path,
                            revision.toRevision(),
                            copyPath,
                            ChangestructureFactory.createRepoRevision(pathInfo.getCopyRevision(),
                                    revision.getRepository()));
                } else {
                    historyGraph.addReplacement(path, revision.toRevision());
                }
            } else if (pathInfo.isNew()) {
                if (copyPath != null) {
                    historyGraph.addCopy(
                            copyPath,
                            path,
                            ChangestructureFactory.createRepoRevision(pathInfo.getCopyRevision(),
                                    revision.getRepository()),
                            revision.toRevision());
                } else {
                    historyGraph.addAddition(path, revision.toRevision());
                }
            } else {
                historyGraph.addChange(path, revision.toRevision(),
                        Collections.<IRevision>singleton(ChangestructureFactory.createRepoRevision(
                                e.getValue().getAncestorRevision(),
                                revision.getRepository())
                        ));
            }
        }
    }

    private Map<SvnRepo, TreeMap<Long, SvnRevision>> groupResultsByRepository() {
        final Map<SvnRepo, TreeMap<Long, SvnRevision>> result = new LinkedHashMap<>();
        for (final SvnRevision revision : this.potentiallyRelevantEntries) {
            TreeMap<Long, SvnRevision> revsForRepo = result.get(revision.getRepository());
            if (revsForRepo == null) {
                revsForRepo = new TreeMap<>();
                result.put(revision.getRepository(), revsForRepo);
            }
            revsForRepo.put(revision.getRevisionNumber(), revision);
        }
        return result;
    }

}
