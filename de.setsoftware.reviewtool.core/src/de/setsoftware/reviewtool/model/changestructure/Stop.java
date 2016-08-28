package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    private final List<FileInRevision> historyOrder = new ArrayList<>();
    private final Multimap<FileInRevision, Fragment> history = new Multimap<>();

    private final FileInRevision mostRecentFile;
    private final Fragment mostRecentFragment;
    //TODO does this flag make sense? visibility is an attribute of a tour (if at all)
    private final boolean isVisible;

    private final boolean irrelevantForReview;

    /**
     * Constructor for textual changes.
     */
    public Stop(
            Fragment from,
            Fragment to,
            Fragment traceFragment,
            boolean irrelevantForReview,
            final boolean isVisible) {
        this.historyOrder.add(from.getFile());
        this.historyOrder.add(to.getFile());
        this.history.put(from.getFile(), from);
        this.history.put(to.getFile(), to);

        this.mostRecentFile = traceFragment.getFile();
        this.mostRecentFragment = traceFragment;

        this.irrelevantForReview = irrelevantForReview;
        this.isVisible = isVisible;
    }

    /**
     * Constructor for binary changes.
     */
    public Stop(
            FileInRevision from,
            FileInRevision to,
            FileInRevision traceFile,
            boolean irrelevantForReview,
            final boolean isVisible) {
        this.historyOrder.add(from);
        this.historyOrder.add(to);

        this.mostRecentFile = traceFile;
        this.mostRecentFragment = null;

        this.irrelevantForReview = irrelevantForReview;
        this.isVisible = isVisible;
    }

    /**
     * Constructor for internal use.
     */
    private Stop(List<FileInRevision> historyOrder,
            Multimap<FileInRevision, Fragment> history,
            FileInRevision mostRecentFile,
            Fragment mostRecentFragment,
            boolean irrelevantForReview,
            final boolean isVisible) {
        this.historyOrder.addAll(historyOrder);
        this.history.putAll(history);
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

    public List<FileInRevision> getHistory() {
        return this.historyOrder;
    }

    public List<Fragment> getContentFor(FileInRevision revision) {
        return this.history.get(revision);
    }

    /**
     * Return true iff this stop can be merged with the given other stop.
     * Two stops can be merged if they denote the same file and and directly
     * neighboring or overlapping segments of that file (or the whole binary file).
     * Neighboring segments are only considered mergeable if both are either
     * irrelevant or relevant.
     */
    public boolean canBeMergedWith(Stop other) {
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
    public Stop merge(Stop other) {
        assert this.canBeMergedWith(other);

        final LinkedHashSet<FileInRevision> mergedFileSet = new LinkedHashSet<>(this.historyOrder);
        mergedFileSet.addAll(other.historyOrder);
        final List<FileInRevision> mergedHistoryOrder = FileInRevision.sortByRevision(mergedFileSet);

        final Multimap<FileInRevision, Fragment> mergedHistory = new Multimap<>();
        mergedHistory.putAll(this.history);
        mergedHistory.putAll(other.history);

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
    public boolean equals(Object o) {
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
        for (final Entry<FileInRevision, List<Fragment>> e : this.history.entrySet()) {
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
        for (final Fragment f : this.getContentFor(oldestFile)) {
            ret += f.getNumberOfLines();
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
