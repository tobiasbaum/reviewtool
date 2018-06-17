package de.setsoftware.reviewtool.model.api;

import java.util.List;
import java.util.Map;

import de.setsoftware.reviewtool.base.IMultimap;

/**
 * A part of a review tour, corresponding to some notion of "singular change".
 * It knows the file fragment it belongs to in the most current revision, but
 * also the changes that it is based on.
 * <p/>
 * A stop is immutable.
 */
public interface IStop {

    /**
     * Returns the {@link IWorkingCopy} this stop refers to.
     */
    public abstract IWorkingCopy getWorkingCopy();

    /**
     * Returns {@code true} iff a detailed most recent fragment is known.
     */
    public abstract boolean isDetailedFragmentKnown();

    /**
     * Returns the most recent fragment this stop refers to.
     * Local modifications are not considered.
     * @pre {@code this.isDetailedFragmentKnown()}
     */
    public abstract IFragment getOriginalMostRecentFragment();

    /**
     * Returns the most recent file this stop refers to.
     * Local modifications (e.g. move or rename operations) are not considered.
     */
    public abstract IRevisionedFile getOriginalMostRecentFile();

    /**
     * Returns the most recent fragment this stop refers to.
     * Local modifications are considered.
     * @pre {@code this.isDetailedFragmentKnown()}
     */
    public abstract IFragment getMostRecentFragment();

    /**
     * Returns the most recent file this stop refers to.
     * Local modifications (e.g. move or rename operations) are considered.
     */
    public abstract IRevisionedFile getMostRecentFile();

    /**
     * Updates the most recent file and fragment given a {@link IFragmentTracer}.
     * This operation is used to make the stop aware about current local modifications.
     * The original most recent fragment and file are not forgotten, each update uses them as the basis for tracing.
     */
    public abstract void updateMostRecentData(IFragmentTracer tracer);

    /**
     * Returns the revisions relevant for this stop, as a map with entries
     * in the form (from revision, to revision).
     */
    public abstract Map<IRevisionedFile, IRevisionedFile> getHistory();

    /**
     * Returns the hunks belonging to this stop, accessible by their respective "from" revision.
     */
    public abstract IMultimap<IRevisionedFile, IHunk> getHunks();

    /**
     * Returns the hunks with the given source file/revision.
     */
    public abstract List<? extends IHunk> getContentFor(IRevisionedFile revision);

    /**
     * Returns {@code true} iff this stop represents a binary change.
     */
    public abstract boolean isBinaryChange();

    /**
     * Returns the total number of fragments belonging to this stop.
     * The returned number includes both the old and the new fragment, i.e. the return
     * value for a simple stop is 2.
     */
    public abstract int getNumberOfFragments();

    /**
     * Returns the total count of all added lines (right-hand side of a stop).
     * A change is counted as both remove and add.
     */
    public abstract int getNumberOfAddedLines();

    /**
     * Returns the total count of all removed lines (left-hand side of a stop).
     * A change is counted as both remove and add.
     */
    public abstract int getNumberOfRemovedLines();

    /**
     * Returns true iff this stop was classified as irrelevant for code review.
     */
    public abstract boolean isIrrelevantForReview();
}
