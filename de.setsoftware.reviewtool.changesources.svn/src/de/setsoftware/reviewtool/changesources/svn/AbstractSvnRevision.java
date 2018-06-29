package de.setsoftware.reviewtool.changesources.svn;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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
        this.integrateInto(graph, new ArrayList<>(this.getChangedPaths().entrySet()), false);
    }

    /**
     * Processes a single SVN repository revision by translating it into a file history graph operation.
     *
     * @param revision The revision to process,
     * @param deferredBatches Copy operations to be processed later.
     * @param skipFirstCopy If {@code true}, the first copy operation is processed "as is", else it is deferred.
     */
    private void integrateInto(
            final IMutableFileHistoryGraph graph,
            final List<Map.Entry<String, CachedLogEntryPath>> entries,
            final boolean skipFirstCopy) {

        final List<List<Map.Entry<String, CachedLogEntryPath>>> deferredCopies = new ArrayList<>();
        final ListIterator<Map.Entry<String, CachedLogEntryPath>> it = entries.listIterator();
        boolean firstEntryProcessed = false;

        while (it.hasNext()) {
            final Map.Entry<String, CachedLogEntryPath> e = it.next();
            final String path = e.getKey();
            final CachedLogEntryPath pathInfo = e.getValue();
            final String copyPath = pathInfo.getCopyPath();

            if (pathInfo.isReplaced()) {
                if (copyPath != null) {
                    if (skipFirstCopy && !firstEntryProcessed) {
                        graph.addDeletion(path, this.toRevision());
                        graph.addCopy(
                                copyPath,
                                ChangestructureFactory.createRepoRevision(
                                        ComparableWrapper.wrap(pathInfo.getCopyRevision()),
                                        this.getRepository()),
                                path,
                                this.toRevision());
                        firstEntryProcessed = true;
                    } else {
                        deferredCopies.add(this.deferCopy(e, it));
                    }
                } else {
                    graph.addDeletion(path, this.toRevision());
                    graph.addAddition(path, this.toRevision());
                }
            } else if (pathInfo.isDeleted()) {
                graph.addDeletion(path, this.toRevision());
            } else if (pathInfo.isNew()) {
                if (copyPath != null) {
                    if (skipFirstCopy && !firstEntryProcessed) {
                        graph.addCopy(
                                copyPath,
                                ChangestructureFactory.createRepoRevision(
                                        ComparableWrapper.wrap(pathInfo.getCopyRevision()),
                                        this.getRepository()),
                                path,
                                this.toRevision());
                        firstEntryProcessed = true;
                    } else {
                        deferredCopies.add(this.deferCopy(e, it));
                    }
                } else {
                    graph.addAddition(path, this.toRevision());
                }
            } else {
                graph.addChange(
                        path,
                        this.toRevision(),
                        Collections.singleton(ChangestructureFactory.createRepoRevision(
                                ComparableWrapper.wrap(e.getValue().getAncestorRevision()),
                                this.getRepository())));
            }
        }

        for (final List<Map.Entry<String, CachedLogEntryPath>> deferredCopy : deferredCopies) {
            this.integrateInto(graph, deferredCopy, true);
        }
    }

    /**
     * Defers a copy target and all entries that refer to a subpath of the copy target.
     * @param firstEntry The first entry denoting the copy operation.
     * @param it The entry iterator.
     * @return The batch containing all deferred entries.
     */
    private List<Map.Entry<String, CachedLogEntryPath>> deferCopy(
            final Map.Entry<String, CachedLogEntryPath> firstEntry,
            final ListIterator<Map.Entry<String, CachedLogEntryPath>> it) {

        final List<Map.Entry<String, CachedLogEntryPath>> batch = new ArrayList<>();
        batch.add(firstEntry);

        while (it.hasNext()) {
            final Map.Entry<String, CachedLogEntryPath> entry = it.next();
            if (Paths.get(entry.getKey()).startsWith(firstEntry.getKey())) {
                batch.add(entry);
            } else {
                it.previous();
                return batch;
            }
        }

        return batch;
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
