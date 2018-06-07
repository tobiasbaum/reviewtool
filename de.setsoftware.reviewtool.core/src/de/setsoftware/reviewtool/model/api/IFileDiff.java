package de.setsoftware.reviewtool.model.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Represents a set of difference hunks between different revisions of the same file. A FileDiff object can accumulate
 * hunks of different source/target revisions.
 */
public interface IFileDiff extends Serializable {

    /**
     * Returns un unmodifiable view of the hunks this {@link IFileDiff} is made of.
     */
    public abstract List<? extends IHunk> getHunks();

    /**
     * Returns the {@link IRevisionedFile} this diff starts at.
     */
    public abstract IRevisionedFile getFrom();

    /**
     * Returns the {@link IRevisionedFile} this diff ends at.
     */
    public abstract IRevisionedFile getTo();

    /**
     * Creates a copy of this object with a new target revision. The list of hunks is copied in such a way that
     * both lists can be modified separately after the copy.
     *
     * @param newTo The new target revision.
     * @return The requested copy.
     */
    public abstract IFileDiff setTo(IRevisionedFile newTo);

    /**
     * Traces a source fragment over the recorded hunks to the last known file revision.
     * @param source The source fragment.
     * @return The resulting fragment matching the last known file revision.
     */
    public abstract IFragment traceFragment(IFragment source);

    /**
     * Returns a list of hunks related to a collection of target {@link IFragment}s. A hunk is related if its target
     * fragment originates from some fragment that overlaps or is adjacent to at least one of the fragments passed.
     */
    public abstract List<? extends IHunk> getHunksWithTargetChangesInOneOf(Collection<? extends IFragment> fragments);

    /**
     * Merges a hunk into this {@link IFileDiff}. This is allowed only for hunks that do not overlap with hunks
     * in this {@link IFileDiff}.
     * <p/>
     * The {@link IFileDiff} is not changed, rather a new {@link IFileDiff} is created and returned (value semantics).
     *
     * @param hunkToMerge The hunk to be merged.
     * @return The new {@link IFileDiff} containing the merged hunk.
     * @throws IncompatibleFragmentException if the hunk to be merged overlaps with some hunk in the {@link IFileDiff}.
     */
    public abstract IFileDiff merge(IHunk hunkToMerge) throws IncompatibleFragmentException;

    /**
     * Merges a list of hunks into this {@link IFileDiff}. This operation assumes that the hunks do not know anything
     * about each other, i.e. the source/target fragments refer to the same state of file. Therefore, applying two hunks
     * separately is different from applying them together.
     * <p/>
     * The {@link IFileDiff} is not changed, rather a new {@link IFileDiff} is created and returned (value semantics).
     *
     * @param hunksToMerge A list of hunks to be merged.
     * @return The new {@link IFileDiff} containing the merged hunks.
     * @throws IncompatibleFragmentException if some hunk to be merged overlaps with some hunk in the {@link IFileDiff}.
     */
    public abstract IFileDiff merge(Collection<? extends IHunk> hunksToMerge) throws IncompatibleFragmentException;

    /**
     * Merges a list of hunks of a {@link IFileDiff} into this {@link IFileDiff}.
     * <p/>
     * The {@link IFileDiff} is not changed, rather a new {@link IFileDiff} is created and returned (value semantics).
     *
     * @param diff A {@link IFileDiff} to merge.
     * @return The new {@link IFileDiff} containing the merged hunks.
     * @throws IncompatibleFragmentException if some hunk to be merged overlaps with some hunk in the {@link IFileDiff}.
     */
    public abstract IFileDiff merge(IFileDiff diff) throws IncompatibleFragmentException;

}
