package de.setsoftware.reviewtool.model.api;

import java.util.Set;

/**
 * Represents a mutable file history graph.
 */
public interface IMutableFileHistoryGraph extends IFileHistoryGraph {

    @Override
    public abstract IMutableFileHistoryNode getNodeFor(IRevisionedFile file);

    /**
     * Adds the information that the path {@link path} was added or changed in revision {@link revision}.
     * If the set {@code ancestorRevisions} is not empty, a change is recorded and linked to the ancestors,
     * otherwise an addition is recorded.
     */
    public abstract void addAdditionOrChange(
            String path,
            IRevision revision,
            Set<IRevision> ancestorRevisions);

    /**
     * Adds the information that the file with the given path was deleted with the commit of the given revision.
     * The set {@code ancestorRevisions} may not be empty and denotes the ancestor node(s) where the path existed
     * just before deletion. The deletion node is linked to them, possibly creating intermediate
     * {@link IFileHistoryNode}s just before the deletion. This supports finding the last revision of a file
     * before being deleted.
     */
    public abstract void addDeletion(
            String path,
            IRevision revision,
            Set<IRevision> ancestorRevisions);

    /**
     * Adds the information that the path {@code path} was replaced by a fresh path (file or directory) in revision
     * {@code revision}. The replacement node is linked to passed {@code ancestorRevisions}, possibly creating
     * intermediate {@link IFileHistoryNode}s just before the deletion. This supports finding the last revision of a
     * path before being deleted.
     */
    public abstract void addReplacement(
            String path,
            IRevision revision,
            Set<IRevision> ancestorRevisions);

    /**
     * Adds the information that the path {@code path} was replaced by a fresh path (file or directory) in revision
     * {@code revision}, copied from {@code pathFrom} at revision {@code revisionFrom}. The replacement node is linked
     * to passed {@code ancestorRevisions ancestors}, possibly creating intermediate {@link IFileHistoryNode}s
     * just before the deletion. This supports finding the last revision of a path before being deleted.
     */
    public abstract void addReplacement(
            String path,
            IRevision revision,
            Set<IRevision> ancestorRevisions,
            String pathFrom,
            IRevision revisionFrom);

    /**
     * Adds the information that the path {@code pathFrom} at revision {@code revisionFrom} was copied to
     * path {@code pathTo} in revision {@code revisionTo}.
     */
    public abstract void addCopy(
            String pathFrom,
            String pathTo,
            IRevision revisionFrom,
            IRevision revisionTo);

}
