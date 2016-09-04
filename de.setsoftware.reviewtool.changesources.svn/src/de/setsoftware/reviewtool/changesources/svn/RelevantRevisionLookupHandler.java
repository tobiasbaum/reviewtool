package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;

import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Handler that filters log entries with a given pattern.
 * Also determines entries that are relevant to restore the full history of files in matching log entries.
 * These revisions have to be considered for a consistent history as otherwise consolidation of diffs is
 * inaccurate. Revisions that are retrofitted are marked as "invisible" in order to be able to differentiate between
 * "proper" and "technically necessary" revisions.
 */
class RelevantRevisionLookupHandler implements ISVNLogEntryHandler {

    private final Pattern pattern;
    private final List<SvnRevision> potentiallyRelevantEntries = new ArrayList<>();
    private final List<SvnRevision> entriesSinceLastMatching = new ArrayList<>();
    private SvnRepo currentRoot;

    public RelevantRevisionLookupHandler(Pattern patternForKey) {
        this.pattern = patternForKey;
    }

    public void setCurrentRepo(SvnRepo repo) {
        this.currentRoot = repo;
        this.entriesSinceLastMatching.clear();
    }

    @Override
    public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
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
    public List<SvnRevision> determineRelevantRevisions(FileHistoryGraph historyGraphBuffer) {
        final List<SvnRevision> ret = new ArrayList<>();
        for (final TreeMap<Long, SvnRevision> revisionsInRepo : this.groupResultsByRepository().values()) {
            //iterate over revisions in order and keep track of the relevant paths
            final Set<String> relevantPaths = new HashSet<>();
            for (final SvnRevision revision : revisionsInRepo.values()) {
                if (revision.isVisible()) {
                    ret.add(revision);
                    this.adjustRelevantPaths(revision, relevantPaths, historyGraphBuffer);
                } else {
                    if (this.touchesRelevantPath(revision, relevantPaths)) {
                        ret.add(revision);
                        this.trackMovesAndCopies(revision, relevantPaths, historyGraphBuffer);
                    }
                }
            }
        }
        return ret;
    }

    private void adjustRelevantPaths(
            SvnRevision revision, Set<String> relevantPaths, FileHistoryGraph historyGraphBuffer) {
        assert revision.isVisible();
        for (final Entry<String, SVNLogEntryPath> e : revision.getChangedPaths().entrySet()) {
            if (e.getValue().getKind() != SVNNodeKind.FILE) {
                continue;
            }
            if (e.getValue().getType() == SVNLogEntryPath.TYPE_DELETED) {
                relevantPaths.remove(e.getKey());
                historyGraphBuffer.addDeletion(
                        e.getKey(),
                        ChangestructureFactory.createRepoRevision(revision.getRevision()),
                        revision.getRepository());
            } else {
                relevantPaths.add(e.getKey());
                if (e.getValue().getCopyPath() != null) {
                    historyGraphBuffer.addCopy(
                            e.getValue().getCopyPath(),
                            e.getKey(),
                            ChangestructureFactory.createRepoRevision(e.getValue().getCopyRevision()),
                            ChangestructureFactory.createRepoRevision(revision.getRevision()),
                            revision.getRepository());
                }
            }
        }
    }

    private void trackMovesAndCopies(
            SvnRevision revision, Set<String> relevantPaths, FileHistoryGraph historyGraphBuffer) {
        assert !revision.isVisible();
        for (final Entry<String, SVNLogEntryPath> e : revision.getChangedPaths().entrySet()) {
            final String copyPath = e.getValue().getCopyPath();
            if (copyPath != null && relevantPaths.contains(copyPath)) {
                relevantPaths.add(e.getKey());
                historyGraphBuffer.addCopy(
                        e.getValue().getCopyPath(),
                        e.getKey(),
                        ChangestructureFactory.createRepoRevision(e.getValue().getCopyRevision()),
                        ChangestructureFactory.createRepoRevision(revision.getRevision()),
                        revision.getRepository());
            }
        }
        for (final Entry<String, SVNLogEntryPath> e : revision.getChangedPaths().entrySet()) {
            if (e.getValue().getType() == SVNLogEntryPath.TYPE_DELETED) {
                relevantPaths.remove(e.getKey());
                historyGraphBuffer.addDeletion(
                        e.getKey(),
                        ChangestructureFactory.createRepoRevision(revision.getRevision()),
                        revision.getRepository());
            }
        }
    }

    private boolean touchesRelevantPath(SvnRevision revision, Set<String> relevantPaths) {
        return !Collections.disjoint(relevantPaths, revision.getChangedPaths().keySet());
    }

    private Map<SvnRepo, TreeMap<Long, SvnRevision>> groupResultsByRepository() {
        final Map<SvnRepo, TreeMap<Long, SvnRevision>> result = new LinkedHashMap<>();
        for (final SvnRevision revision : this.potentiallyRelevantEntries) {
            TreeMap<Long, SvnRevision> revsForRepo = result.get(revision.getRepository());
            if (revsForRepo == null) {
                revsForRepo = new TreeMap<>();
                result.put(revision.getRepository(), revsForRepo);
            }
            revsForRepo.put(revision.getRevision(), revision);
        }
        return result;
    }

}