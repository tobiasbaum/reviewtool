package de.setsoftware.reviewtool.model.api;

import java.util.Set;

/**
 * Represents a mutable file history graph.
 */
public interface IMutableFileHistoryGraph extends IFileHistoryGraph {

    @Override
    public abstract IMutableFileHistoryNode getNodeFor(IRevisionedFile file);

    /**
     * Adds the information that the path {@link path} was added in revision {@link revision}.
     */
    public abstract void addAddition(
            String path,
            IRevision revision);

    /**
     * Adds the information that the path {@link path} was changed in revision {@link revision}.
     *
     * @param ancestorRevisions
     *      The ancestor revisions of {@link revision}. Used if no other ancestor is known (e.g. due to a copy).
     */
    public abstract void addChange(
            String path,
            IRevision revision,
            Set<? extends IRevision> ancestorRevisions);

    /**
     * Adds the information that the file with the given path was deleted with the commit of the given revision.
     */
    public abstract void addDeletion(
            String path,
            IRevision revision);

    /**
     * Adds the information that the path {@code path} was replaced by a fresh path (file or directory) in revision
     * {@code revision}.
     */
    public abstract void addReplacement(
            String path,
            IRevision revision);

    /**
     * Adds the information that the path {@code path} was replaced by a fresh path (file or directory) in revision
     * {@code revision}, copied from {@code pathFrom} at revision {@code revisionFrom}.
     */
    public abstract void addReplacement(
            String path,
            IRevision revision,
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
