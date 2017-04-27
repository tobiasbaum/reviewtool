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

import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.IChangeSourceUi;
import de.setsoftware.reviewtool.model.changestructure.Revision;

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

    public RelevantRevisionLookupHandler(Pattern patternForKey) {
        this.pattern = patternForKey;
    }

    @Override
    public void startNewRepo(SvnRepo repo) {
        this.currentRoot = repo;
        this.entriesSinceLastMatching.clear();
    }

    @Override
    public void handleLogEntry(CachedLogEntry logEntry) throws SVNException {
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
    public List<ISvnRevision> determineRelevantRevisions(final FileHistoryGraph historyGraphBuffer,
            final IChangeSourceUi ui) {
        final List<ISvnRevision> ret = new ArrayList<>();
        for (final TreeMap<Long, SvnRevision> revisionsInRepo : this.groupResultsByRepository().values()) {
            for (final SvnRevision revision : revisionsInRepo.values()) {
                if (ui.isCanceled()) {
                    throw new OperationCanceledException();
                }
                if (processRevision(revision, historyGraphBuffer)) {
                    ret.add(revision);
                }
            }
        }
        return ret;
    }

    private static Revision toRevision(final long revision) {
        if (revision == Long.MAX_VALUE) {
            return ChangestructureFactory.createLocalRevision();
        } else {
            return ChangestructureFactory.createRepoRevision(revision);
        }
    }

    public static boolean processRevision(final ISvnRevision revision, final FileHistoryGraph historyGraphBuffer) {
        boolean isRelevant = false;
        for (final Entry<String, CachedLogEntryPath> e : revision.getChangedPaths().entrySet()) {
            final String path = e.getKey();
            if (e.getValue().isDeleted()) {
                if (revision.isVisible() || historyGraphBuffer.contains(path, revision.getRepository())) {
                    historyGraphBuffer.addDeletion(
                            path,
                            revision.toRevision(),
                            Collections.<Revision>singleton(toRevision(e.getValue().getAncestorRevision())),
                            revision.getRepository());
                    isRelevant = true;
                }
            } else {
                final String copyPath = e.getValue().getCopyPath();
                if (copyPath != null
                        && (revision.isVisible() || historyGraphBuffer.contains(copyPath, revision.getRepository()))) {
                    historyGraphBuffer.addCopy(
                            copyPath,
                            path,
                            ChangestructureFactory.createRepoRevision(e.getValue().getAncestorRevision()),
                            revision.toRevision(),
                            revision.getRepository());
                    isRelevant = true;
                } else if (e.getValue().isFile()
                        && (revision.isVisible() || historyGraphBuffer.contains(path, revision.getRepository()))) {
                    if (e.getValue().isNew()) {
                        historyGraphBuffer.addAdditionOrChange(
                                path,
                                revision.toRevision(),
                                Collections.<Revision>emptySet(),
                                revision.getRepository());
                    } else {
                        historyGraphBuffer.addAdditionOrChange(
                                path,
                                revision.toRevision(),
                                Collections.<Revision>singleton(toRevision(e.getValue().getAncestorRevision())),
                                revision.getRepository());
                    }
                    isRelevant = true;
                }
            }
        }
        return isRelevant;
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