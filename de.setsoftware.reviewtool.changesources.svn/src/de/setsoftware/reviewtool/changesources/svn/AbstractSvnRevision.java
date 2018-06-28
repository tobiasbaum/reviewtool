package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import de.setsoftware.reviewtool.base.ComparableWrapper;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Common behaviour for {@link SvnRevision} implementations.
 */
abstract class AbstractSvnRevision implements SvnRevision {

    /**
     * Processes a single SVN repository revision by translating it into a file history graph operation.
     *
     * @param revision The revision to process,
     */
    void integrateInto(final IMutableFileHistoryGraph graph) {
        for (final Entry<String, CachedLogEntryPath> e : this.getChangedPaths().entrySet()) {
            final String path = e.getKey();
            final CachedLogEntryPath pathInfo = e.getValue();

            if (pathInfo.isDeleted() || pathInfo.isReplaced()) {
                graph.addDeletion(path, this.toRevision());
            }

            if (pathInfo.isNew() || pathInfo.isReplaced()) {
                final String copyPath = pathInfo.getCopyPath();
                if (copyPath != null) {
                    graph.addCopy(
                            copyPath,
                            path,
                            ChangestructureFactory.createRepoRevision(
                                    ComparableWrapper.wrap(pathInfo.getCopyRevision()),
                                    this.getRepository()),
                            this.toRevision());
                } else {
                    graph.addAddition(path, this.toRevision());
                }
            }

            if (!pathInfo.isDeleted() && !pathInfo.isNew()  && !pathInfo.isReplaced()) {
                graph.addChange(
                        path,
                        this.toRevision(),
                        Collections.singleton(ChangestructureFactory.createRepoRevision(
                                ComparableWrapper.wrap(e.getValue().getAncestorRevision()),
                                this.getRepository())));
            }
        }
    }

    /**
     * Filters out all changed paths that do not belong to passed working copy.
     * @param paths The paths to filter.
     * @param wc The working copy.
     * @return A map of path entries that belong to passed working copy.
     */
    protected final SortedMap<String, CachedLogEntryPath> filterPaths(final SvnWorkingCopy wc) {

        final SortedMap<String, CachedLogEntryPath> result = new TreeMap<>();
        final Iterator<Map.Entry<String, CachedLogEntryPath>> it = this.getChangedPaths().entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, CachedLogEntryPath> entry = it.next();
            if (wc.toAbsolutePathInWc(entry.getKey()) != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }
}
