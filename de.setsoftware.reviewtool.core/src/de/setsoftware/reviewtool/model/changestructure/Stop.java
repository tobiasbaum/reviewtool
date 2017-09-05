package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Util;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IFragmentTracer;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.ITextualChange;

/**
 * A part of a review tour, corresponding to some notion of "singular change".
 * It knows the file fragment it belongs to in the most current revision, but
 * also the changes that it is based on.
 * <p/>
 * A stop is immutable.
 */
public class Stop extends TourElement {

    private final Map<IRevisionedFile, IRevisionedFile> historyOrder;
    private final Multimap<IRevisionedFile, Hunk> history;

    private final IRevisionedFile mostRecentFile;
    private final IFragment mostRecentFragment;
    private transient IRevisionedFile mostRecentFileConsideringLocalChanges;
    private transient IFragment mostRecentFragmentConsideringLocalChanges;

    private final boolean irrelevantForReview;

    /**
     * Constructor for textual changes.
     */
    public Stop(
            final ITextualChange change,
            final IFragment traceFragment) {
        this.historyOrder = new LinkedHashMap<>();
        this.historyOrder.put(change.getFrom(), change.getTo());
        this.history = new Multimap<>();
        this.history.put(change.getFrom(), new Hunk(change));

        this.mostRecentFile = traceFragment.getFile();
        this.mostRecentFragment = traceFragment;
        this.mostRecentFileConsideringLocalChanges = null;
        this.mostRecentFragmentConsideringLocalChanges = null;

        this.irrelevantForReview = change.isIrrelevantForReview();
        assert change.isVisible();
    }

    /**
     * Constructor for binary changes.
     */
    public Stop(
            final IBinaryChange change,
            final IRevisionedFile traceFile) {
        this.historyOrder = new LinkedHashMap<>();
        this.historyOrder.put(change.getFrom(), change.getTo());
        this.history = new Multimap<>();

        this.mostRecentFile = traceFile;
        this.mostRecentFragment = null;
        this.mostRecentFileConsideringLocalChanges = null;
        this.mostRecentFragmentConsideringLocalChanges = null;

        this.irrelevantForReview = change.isIrrelevantForReview();
        assert change.isVisible();
    }

    /**
     * Constructor for internal use.
     */
    private Stop(
            final Map<IRevisionedFile, IRevisionedFile> historyOrder,
            final Multimap<IRevisionedFile, Hunk> history,
            final IRevisionedFile mostRecentFile,
            final IFragment mostRecentFragment,
            final IRevisionedFile mostRecentFileConsideringLocalChanges,
            final IFragment mostRecentFragmentConsideringLocalChanges,
            final boolean irrelevantForReview) {
        this.historyOrder = historyOrder;
        this.history = history;
        this.mostRecentFile = mostRecentFile;
        this.mostRecentFragment = mostRecentFragment;
        this.mostRecentFileConsideringLocalChanges = mostRecentFileConsideringLocalChanges;
        this.mostRecentFragmentConsideringLocalChanges = mostRecentFragmentConsideringLocalChanges;
        this.irrelevantForReview = irrelevantForReview;
    }

    public boolean isDetailedFragmentKnown() {
        return this.mostRecentFragment != null;
    }

    public IFragment getOriginalMostRecentFragment() {
        return this.mostRecentFragment;
    }

    public IRevisionedFile getOriginalMostRecentFile() {
        return this.mostRecentFile;
    }

    public IFragment getMostRecentFragment() {
        return this.mostRecentFragmentConsideringLocalChanges != null ? this.mostRecentFragmentConsideringLocalChanges
                : this.mostRecentFragment;
    }

    public IRevisionedFile getMostRecentFile() {
        return this.mostRecentFileConsideringLocalChanges != null ? this.mostRecentFileConsideringLocalChanges
                : this.mostRecentFile;
    }

    /**
     * Updates the most recent file and fragment given a {@link IFragmentTracer}.
     * This operation is used to make the stop aware about current local modifications.
     * The original most recent fragment and file are not forgotten, each update uses them as the basis for tracing.
     */
    public void updateMostRecentData(final IFragmentTracer tracer) {
        if (this.mostRecentFragment != null) {
            final List<? extends IFragment> fragments = tracer.traceFragment(this.mostRecentFragment);
            if (!fragments.isEmpty()) {
                this.mostRecentFragmentConsideringLocalChanges = fragments.get(0);
            }
        }
        final List<IRevisionedFile> files = tracer.traceFile(this.mostRecentFile);
        if (!files.isEmpty()) {
            this.mostRecentFileConsideringLocalChanges = files.get(0);
        }
    }

    /**
     * Returns the revisions relevant for this stop, as a map with entries
     * in the form (from revision, to revision).
     */
    public Map<IRevisionedFile, IRevisionedFile> getHistory() {
        return Collections.unmodifiableMap(this.historyOrder);
    }

    public List<Hunk> getContentFor(final IRevisionedFile revision) {
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

        final LinkedHashMap<IRevisionedFile, IRevisionedFile> mergedHistoryOrder =
                new LinkedHashMap<>(this.historyOrder);
        mergedHistoryOrder.putAll(other.historyOrder);

        final Multimap<IRevisionedFile, Hunk> mergedHistory = new Multimap<>();
        mergedHistory.putAll(this.history);
        mergedHistory.putAll(other.history);
        mergedHistory.sortValues();

        return new Stop(
                mergedHistoryOrder,
                mergedHistory,
                this.mostRecentFile,
                this.mostRecentFragment == null ? null : this.mostRecentFragment.merge(other.mostRecentFragment),
                this.mostRecentFileConsideringLocalChanges,
                this.mostRecentFragmentConsideringLocalChanges == null ? null
                        : this.mostRecentFragmentConsideringLocalChanges.merge(
                                other.mostRecentFragmentConsideringLocalChanges),
                //the result is only irrelevant if both parts are irrelevant. It would probably be more accurate
                //  to track which part is relevant and which is not (so that a merge could result in multiple
                //  stops), but this complicates some algorithms and is only useful for large irrelevant stops,
                //  which should be quite rare
                this.irrelevantForReview && other.irrelevantForReview);
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
     * The returned number includes both the old and the new fragment, i.e. the return
     * value for a simple stop is 2.
     */
    public int getNumberOfFragments() {
        int ret = 1;
        for (final Entry<IRevisionedFile, List<Hunk>> e : this.history.entrySet()) {
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
        int ret = 0;
        for (final IRevisionedFile oldestFile : this.getHistoryRoots()) {
            for (final IHunk hunk : this.getContentFor(oldestFile)) {
                ret += hunk.getSource().getNumberOfLines();
            }
        }
        return ret;
    }

    private Set<IRevisionedFile> getHistoryRoots() {
        final Set<IRevisionedFile> ret = new LinkedHashSet<>(this.historyOrder.keySet());
        ret.removeAll(this.historyOrder.values());
        return ret;
    }

    /**
     * Returns true if this stop was classified as irrelevant for code review.
     */
    public boolean isIrrelevantForReview() {
        return this.irrelevantForReview;
    }

}
