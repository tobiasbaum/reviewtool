package de.setsoftware.reviewtool.model.api;

import java.util.Set;

import de.setsoftware.reviewtool.model.changestructure.FileHistoryNode;

/**
 * Represents a mutable file history graph.
 */
public interface IMutableFileHistoryGraph extends IFileHistoryGraph {

    @Override
    public abstract IMutableFileHistoryNode getNodeFor(IRevisionedFile file);

    /**
     * Adds the information that the file with the given path was added or changed at the commit of the given revision.
     */
    public abstract void addAdditionOrChange(
            String path,
            IRevision revision,
            Set<IRevision> ancestorRevisions,
            IRepository repo);

    /**
     * Adds the information that the file with the given path was deleted with the commit of the given revision.
     * If ancestor nodes exist, the deletion node of type {@link NonExistingFileHistoryNode} is linked to them,
     * possibly creating intermediate {@link FileHistoryNode}s just before the deletion. This supports
     * finding the last revision of a file before being deleted.
     */
    public abstract void addDeletion(
            String path,
            IRevision revision,
            Set<IRevision> ancestorRevisions,
            IRepository repo);

    /**
     * Adds the information that the file with the given "from" path was copied with the commit of the given revision
     * to the given "to" path.
     */
    public abstract void addCopy(
            String pathFrom,
            String pathTo,
            IRevision revisionFrom,
            IRevision revisionTo,
            IRepository repo);

}
