package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Util;

/**
 * A part of a review tour, corresponding to some notion of "singular change".
 * It knows the file fragment it belongs to in the most current revision, but
 * also the changes that it is based on.
 * <p/>
 * A stop is immutable.
 */
public class Stop implements IReviewElement {

    private final Map<FileInRevision, FileInRevision> historyOrder;
    private final Multimap<FileInRevision, Hunk> history;

    private final FileInRevision mostRecentFile;
    private final Fragment mostRecentFragment;
    //TODO does this flag make sense? visibility is an attribute of a tour (if at all)
    private final boolean isVisible;

    private final boolean irrelevantForReview;

    /**
     * Constructor for textual changes.
     */
    public Stop(
            final TextualChangeHunk change,
            final Fragment traceFragment) {
        this.historyOrder = new LinkedHashMap<>();
        this.historyOrder.put(change.getFrom(), change.getTo());
        this.history = new Multimap<>();
        this.history.put(change.getFrom(), new Hunk(change));

        this.mostRecentFile = traceFragment.getFile();
        this.mostRecentFragment = traceFragment;

        this.irrelevantForReview = change.isIrrelevantForReview();
        this.isVisible = change.isVisible();
    }

    /**
     * Constructor for binary changes.
     */
    public Stop(
            final BinaryChange change,
            final FileInRevision traceFile) {
        this.historyOrder = new LinkedHashMap<>();
        this.historyOrder.put(change.getFrom(), change.getTo());
        this.history = new Multimap<>();

        this.mostRecentFile = traceFile;
        this.mostRecentFragment = null;

        this.irrelevantForReview = change.isIrrelevantForReview();
        this.isVisible = change.isVisible();
    }

    /**
     * Constructor for internal use.
     */
    private Stop(
            final Map<FileInRevision, FileInRevision> historyOrder,
            final Multimap<FileInRevision, Hunk> history,
            final FileInRevision mostRecentFile,
            final Fragment mostRecentFragment,
            final boolean irrelevantForReview,
            final boolean isVisible) {
        this.historyOrder = historyOrder;
        this.history = history;
        this.mostRecentFile = mostRecentFile;
        this.mostRecentFragment = mostRecentFragment;
        this.irrelevantForReview = irrelevantForReview;
        this.isVisible = isVisible;
    }

    @Override
    public boolean isVisible() {
        return this.isVisible;
    }

    public boolean isDetailedFragmentKnown() {
        return this.mostRecentFragment != null;
    }

    public Fragment getMostRecentFragment() {
        return this.mostRecentFragment;
    }

    public FileInRevision getMostRecentFile() {
        return this.mostRecentFile;
    }

    public Map<FileInRevision, FileInRevision> getHistory() {
        return Collections.unmodifiableMap(this.historyOrder);
    }

    public List<Hunk> getContentFor(final FileInRevision revision) {
        return this.history.get(revision);
    }

    /**
     * Returns {@code true} if this stop represents a binary change.
     */
    public boolean isBinaryChange() {
        return this.history.isEmpty();
    }

    /**
     * Return true iff this stop can be merged with the given other stop.
     * Two stops can be merged if they denote the same file and directly
     * neighboring or overlapping segments of that file (or the whole binary file).
     * Neighboring segments are only considered mergeable if both are either
     * irrelevant or relevant.
     */
    public boolean canBeMergedWith(final Stop other) {
        if (!this.mostRecentFile.equals(other.mostRecentFile)) {
            return false;
        }
        if (this.mostRecentFragment == null) {
            if (other.mostRecentFragment == null) {
                return true;
            } else {
                return false;
            }
        } else {
            if (other.mostRecentFragment == null) {
                return false;
            } else {
                final boolean fragmentsMergeable =
                        this.mostRecentFragment.canBeMergedWith(other.mostRecentFragment);
                if (this.irrelevantForReview == other.irrelevantForReview) {
                    return fragmentsMergeable;
                } else {
                    return fragmentsMergeable && !this.mostRecentFragment.isNeighboring(other.mostRecentFragment);
                }
            }
        }
    }

    /**
     * Merges this stop with the given other stop.
     * The resulting stop has a potentially larger most recent fragment, but
     * all the detail information of both stops is still contained in the history.
     */
    public Stop merge(final Stop other) {
        assert this.canBeMergedWith(other);

        final LinkedHashMap<FileInRevision, FileInRevision> mergedHistoryOrder = new LinkedHashMap<>(this.historyOrder);
        mergedHistoryOrder.putAll(other.historyOrder);

        final Multimap<FileInRevision, Hunk> mergedHistory = new Multimap<>();
        mergedHistory.putAll(this.history);
        mergedHistory.putAll(other.history);
        mergedHistory.sortValues();

        return new Stop(
                mergedHistoryOrder,
                mergedHistory,
                this.mostRecentFile,
                this.mostRecentFragment == null ? null : this.mostRecentFragment.merge(other.mostRecentFragment),
                //the result is only irrelevant if both parts are irrelevant. It would probably be more accurate
                //  to track which part is relevant and which is not (so that a merge could result in multiple
                //  stops), but this complicates some algorithms and is only useful for large irrelevant stops,
                //  which should be quite rare
                this.irrelevantForReview && other.irrelevantForReview,
                this.isVisible || other.isVisible);
    }

    @Override
    public String toString() {
        return "Stop " + (this.irrelevantForReview ? "(irr.)" : "") + " at "
                + (this.mostRecentFragment != null ? this.mostRecentFragment : this.mostRecentFile);
    }

    @Override
    public int hashCode() {
        return this.mostRecentFragment != null ? this.mostRecentFragment.hashCode() : this.mostRecentFile.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Stop)) {
            return false;
        }
        final Stop s = (Stop) o;
        return this.mostRecentFile.equals(s.mostRecentFile)
            && Util.sameOrEquals(this.mostRecentFragment, s.mostRecentFragment)
            && this.historyOrder.equals(s.historyOrder)
            && this.history.equals(s.history)
            && this.irrelevantForReview == s.irrelevantForReview;
    }

    public File getAbsoluteFile() {
        return this.getMostRecentFile().toLocalPath().toFile().getAbsoluteFile();
    }

    /**
     * Returns the total number of fragments belonging to this stop.
     */
    public int getNumberOfFragments() {
        int ret = 0;
        for (final Entry<FileInRevision, List<Hunk>> e : this.history.entrySet()) {
            ret += e.getValue().size();
        }
        return ret;
    }

    /**
     * Returns the total count of all added lines (right-hand side of a stop).
     * A change is counted as both remove and add.
     */
    public int getNumberOfAddedLines() {
        return this.mostRecentFragment == null ? 0 : this.mostRecentFragment.getNumberOfLines();
    }

    /**
     * Returns the total count of all removed lines (left-hand side of a stop).
     * A change is counted as both remove and add.
     */
    public int getNumberOfRemovedLines() {
        final FileInRevision oldestFile = this.historyOrder.get(0);
        int ret = 0;
        for (final Hunk hunk : this.getContentFor(oldestFile)) {
            ret += hunk.getSource().getNumberOfLines();
        }
        return ret;
    }

    /**
     * Returns true if this stop was classified as irrelevant for code review.
     */
    public boolean isIrrelevantForReview() {
        return this.irrelevantForReview;
    }

}
