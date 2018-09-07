package de.setsoftware.reviewtool.changesources.svn;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;

import de.setsoftware.reviewtool.changesources.core.IScmChange;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Basic implementation for all changes in Subversion.
 */
abstract class SvnChange implements IScmChange<SvnChangeItem>, Serializable {

    private static final long serialVersionUID = -4008355008283598946L;
    private final SortedMap<String, SvnChangeItem> changeItems;

    /**
     * Constructor.
     * @param changeItems The change items.
     */
    SvnChange(final SortedMap<String, SvnChangeItem> changeItems) {
        this.changeItems = changeItems;
    }

    @Override
    public final SortedMap<String, SvnChangeItem> getChangedItems() {
        return Collections.unmodifiableSortedMap(this.changeItems);
    }

    @Override
    public final void integrateInto(final IMutableFileHistoryGraph graph) {
        this.integrateInto(graph, new ArrayList<>(this.changeItems.entrySet()), false);
    }

    /**
     * Processes a set of change items by translating it into a file history graph operation.
     *
     * @param revision The revision to process,
     * @param deferredBatches Copy operations to be processed later.
     * @param skipFirstCopy If {@code true}, the first copy operation is processed "as is", else it is deferred.
     */
    private void integrateInto(
            final IMutableFileHistoryGraph graph,
            final List<Map.Entry<String, SvnChangeItem>> entries,
            final boolean skipFirstCopy) {

        final IRevision revision = this.toRevision();
        final List<List<Map.Entry<String, SvnChangeItem>>> deferredCopies = new ArrayList<>();
        final ListIterator<Map.Entry<String, SvnChangeItem>> it = entries.listIterator();
        final boolean firstEntryProcessed = false;

        while (it.hasNext()) {
            final Map.Entry<String, SvnChangeItem> e = it.next();
            final String path = e.getKey();
            final SvnChangeItem pathInfo = e.getValue();
            final String copyPath = pathInfo.getCopyPath();

            if (pathInfo.isDeleted() && pathInfo.isAdded()) {
                if (copyPath != null) {
                    if (skipFirstCopy && !firstEntryProcessed) {
                        graph.addDeletion(path, revision);
                        graph.addCopy(
                                copyPath,
                                ChangestructureFactory.createRepoRevision(
                                        new SvnCommitId(pathInfo.getCopyRevision()),
                                        revision.getRepository()),
                                path,
                                revision);
                    } else {
                        deferredCopies.add(this.deferCopy(e, it));
                    }
                } else {
                    graph.addDeletion(path, revision);
                    graph.addAddition(path, revision);
                }
            } else if (pathInfo.isDeleted()) {
                graph.addDeletion(path, revision);
            } else if (pathInfo.isAdded()) {
                if (copyPath != null) {
                    if (skipFirstCopy && !firstEntryProcessed) {
                        graph.addCopy(
                                copyPath,
                                ChangestructureFactory.createRepoRevision(
                                        new SvnCommitId(pathInfo.getCopyRevision()),
                                        revision.getRepository()),
                                path,
                                revision);
                    } else {
                        deferredCopies.add(this.deferCopy(e, it));
                    }
                } else {
                    graph.addAddition(path, revision);
                }
            } else if (pathInfo.isChanged()) {
                graph.addChange(
                        path,
                        revision,
                        Collections.singleton(ChangestructureFactory.createRepoRevision(
                                new SvnCommitId(e.getValue().getAncestorRevision()),
                                revision.getRepository())));
            }
        }

        for (final List<Map.Entry<String, SvnChangeItem>> deferredCopy : deferredCopies) {
            this.integrateInto(graph, deferredCopy, true);
        }
    }

    /**
     * Defers a copy target and all entries that refer to a subpath of the copy target.
     * @param firstEntry The first entry denoting the copy operation.
     * @param it The entry iterator.
     * @return The batch containing all deferred entries.
     */
    private List<Map.Entry<String, SvnChangeItem>> deferCopy(
            final Map.Entry<String, SvnChangeItem> firstEntry,
            final ListIterator<Map.Entry<String, SvnChangeItem>> it) {

        final List<Map.Entry<String, SvnChangeItem>> batch = new ArrayList<>();
        batch.add(firstEntry);

        while (it.hasNext()) {
            final Map.Entry<String, SvnChangeItem> entry = it.next();
            if (Paths.get(entry.getKey()).startsWith(firstEntry.getKey())) {
                batch.add(entry);
            } else {
                it.previous();
                return batch;
            }
        }

        return batch;
    }
}
