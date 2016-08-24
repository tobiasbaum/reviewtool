package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.Logger;

/**
 * This class implements an algorithm for completing a possibly fragmented list of revisions. Due to filtering (e.g. by
 * ticket), revisions may be sorted out which touch files that are used in other revisions not being filtered out. For
 * a consistent (= gapless!) history these revisions have to be considered as otherwise consolidation of diffs is
 * inaccurate. Revisions that are retrofitted are marked as "invisible" in order to be able to differentiate between
 * "proper" and "technically necessary" revisions.
 */
public class SvnRevisionCompleter {

    /**
     * Completes a list of revisions.
     * @param mgr The {@link SVNClientManager} to use.
     * @param revisions The list of revisions to complete.
     * @return A completed list of revisions.
     */
    public static List<SvnRevision> complete(final SVNClientManager mgr, final List<SvnRevision> revisions) {
        final Map<SvnRepo, Map<Long, SvnRevision>> revisionMap = groupByRepository(revisions);
        for (final SvnRevision revision : revisions) {
            Map<Long, SvnRevision> revsForRepo = revisionMap.get(revision.getRepository());
            if (revsForRepo == null) {
                revsForRepo = new LinkedHashMap<>();
                revisionMap.put(revision.getRepository(), revsForRepo);
            }
            handleRevision(mgr, revision, revsForRepo);
        }

        return flattenRevisionMap(revisionMap);
    }

    /**
     * Handles a single revision.
     * @param mgr The {@link SVNClientManager} to use.
     * @param revision The {@link SvnRevision} to handle.
     * @param revisionMap Contains revisions already seen.
     */
    private static void handleRevision(final SVNClientManager mgr, final SvnRevision revision,
            final Map<Long, SvnRevision> revisionMap) {
        // overwrite a possibly existing invisible SvnRevision
        revisionMap.put(revision.getRevision(), revision);

        for (final SVNLogEntryPath entryPath : revision.getChangedPaths().values()) {
            if (entryPath.getKind() != SVNNodeKind.FILE || entryPath.getType() == 'D') {
                continue;
            }

            final List<SVNLogEntry> logEntries = new ArrayList<>();
            try {
                mgr.getLogClient().doLog(revision.getRepository().getRemoteUrl(),
                        new String[] { entryPath.getPath() },
                        SVNRevision.create(revision.getRevision()),
                        SVNRevision.create(revision.getRevision()),
                        SVNRevision.HEAD,
                        false,
                        true,
                        0,
                        new ISVNLogEntryHandler() {
                            @Override
                            public void handleLogEntry(final SVNLogEntry entry) {
                                logEntries.add(entry);
                            }
                        });
            } catch (final SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                    // Subversion aborted as the history of the entry ceased to exist; ignore
                    // this error as you cannot say to Subversion "log until deletion" :-(
                } else {
                    Logger.error("reviewtool: SVN exception while handling r" + revision.getRevision() + ": "
                            + e.getMessage(), e);
                }
            }

            for (final SVNLogEntry logEntry : logEntries) {
                final SvnRevision existingRevision = revisionMap.get(logEntry.getRevision());
                if (existingRevision == null) {
                    logEntry.getChangedPaths().clear();
                    logEntry.getChangedPaths().put(entryPath.getPath(), entryPath);
                    revisionMap.put(logEntry.getRevision(), new SvnRevision(revision.getRepository(), logEntry, false));
                } else if (existingRevision.getRevision() != revision.getRevision()) {
                    existingRevision.getChangedPaths().put(entryPath.getPath(), entryPath);
                }
            }
        }
    }

    /**
     * Transforms a list of revisions into a map sorted by repository and revision.
     * @param revisions The list of {@link SvnRevision}s.
     * @return The resulting map.
     */
    private static Map<SvnRepo, Map<Long, SvnRevision>> groupByRepository(final List<SvnRevision> revisions) {
        final Map<SvnRepo, Map<Long, SvnRevision>> result = new LinkedHashMap<>();
        for (final SvnRevision revision : revisions) {
            Map<Long, SvnRevision> revsForRepo = result.get(revision.getRepository());
            if (revsForRepo == null) {
                revsForRepo = new LinkedHashMap<>();
                result.put(revision.getRepository(), revsForRepo);
            }
            revsForRepo.put(revision.getRevision(), revision);
        }
        return result;
    }

    /**
     * Flattens a map of {@link SvnRevision}s sorted by repository and revision number to a list of
     * {@link SvnRevision}s.
     * @param revisionMap The map to flatten.
     * @return The resulting list.
     */
    private static List<SvnRevision> flattenRevisionMap(final Map<SvnRepo, Map<Long, SvnRevision>> revisionMap) {
        final List<SvnRevision> result = new ArrayList<>();
        for (final Map<Long, SvnRevision> revsForRepo : revisionMap.values()) {
            result.addAll(revsForRepo.values());
        }
        return result;
    }
}
