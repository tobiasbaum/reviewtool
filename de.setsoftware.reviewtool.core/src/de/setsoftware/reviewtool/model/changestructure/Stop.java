package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.setsoftware.reviewtool.base.IMultimap;
import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.base.Util;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IFragmentTracer;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IStop;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Implements the {@link IStop} interface.
 *
 */
public class Stop extends TourElement implements IStop {

    private final IWorkingCopy wc;
    private final Map<IRevisionedFile, IRevisionedFile> historyOrder;
    private final Multimap<IRevisionedFile, IHunk> history;

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

        this.wc = change.getWorkingCopy();
        this.historyOrder = new LinkedHashMap<>();
        this.historyOrder.put(change.getFrom(), change.getTo());
        this.history = new Multimap<>();
        this.history.put(change.getFrom(), new Hunk(change));

        this.mostRecentFile = traceFragment.getFile();
        this.mostRecentFragment = traceFragment;
        this.mostRecentFileConsideringLocalChanges = null;
        this.mostRecentFragmentConsideringLocalChanges = null;

        this.irrelevantForReview = change.isIrrelevantForReview();
    }

    /**
     * Constructor for binary changes.
     */
    public Stop(
            final IBinaryChange change,
            final IRevisionedFile traceFile) {

        this.wc = change.getWorkingCopy();
        this.historyOrder = new LinkedHashMap<>();
        this.historyOrder.put(change.getFrom(), change.getTo());
        this.history = new Multimap<>();

        this.mostRecentFile = traceFile;
        this.mostRecentFragment = null;
        this.mostRecentFileConsideringLocalChanges = null;
        this.mostRecentFragmentConsideringLocalChanges = null;

        this.irrelevantForReview = change.isIrrelevantForReview();
    }

    /**
     * Constructor for internal use.
     */
    private Stop(
            final IWorkingCopy wc,
            final Map<IRevisionedFile, IRevisionedFile> historyOrder,
            final Multimap<IRevisionedFile, IHunk> history,
            final IRevisionedFile mostRecentFile,
            final IFragment mostRecentFragment,
            final IRevisionedFile mostRecentFileConsideringLocalChanges,
            final IFragment mostRecentFragmentConsideringLocalChanges,
            final boolean irrelevantForReview) {

        this.wc = wc;
        this.historyOrder = historyOrder;
        this.history = history;
        this.mostRecentFile = mostRecentFile;
        this.mostRecentFragment = mostRecentFragment;
        this.mostRecentFileConsideringLocalChanges = mostRecentFileConsideringLocalChanges;
        this.mostRecentFragmentConsideringLocalChanges = mostRecentFragmentConsideringLocalChanges;
        this.irrelevantForReview = irrelevantForReview;
    }

    @Override
    public IWorkingCopy getWorkingCopy() {
        return this.wc;
    }

    @Override
    public boolean isDetailedFragmentKnown() {
        return this.mostRecentFragment != null;
    }

    @Override
    public IFragment getOriginalMostRecentFragment() {
        return this.mostRecentFragment;
    }

    @Override
    public IRevisionedFile getOriginalMostRecentFile() {
        return this.mostRecentFile;
    }

    @Override
    public synchronized IFragment getMostRecentFragment() {
        return this.mostRecentFragmentConsideringLocalChanges != null ? this.mostRecentFragmentConsideringLocalChanges
                : this.mostRecentFragment;
    }

    @Override
    public synchronized IRevisionedFile getMostRecentFile() {
        return this.mostRecentFileConsideringLocalChanges != null ? this.mostRecentFileConsideringLocalChanges
                : this.mostRecentFile;
    }

    @Override
    public synchronized void updateMostRecentData(final IFragmentTracer tracer) {
        if (this.mostRecentFragment != null) {
            final List<? extends IFragment> fragments =
                    tracer.traceFragment(this.wc.getFileHistoryGraph(), this.mostRecentFragment, true);
            for (final IFragment fragment : fragments) {
                if (this.wc.toAbsolutePathInWc(fragment.getFile().getPath()) != null) {
                    this.mostRecentFragmentConsideringLocalChanges = fragment;
                    break; // we don't support multiple locally changed files per stop
                }
            }
        }
        final List<IRevisionedFile> files = tracer.traceFile(this.wc.getFileHistoryGraph(), this.mostRecentFile, true);
        for (final IRevisionedFile file : files) {
            if (this.wc.toAbsolutePathInWc(file.getPath()) != null) {
                this.mostRecentFileConsideringLocalChanges = file;
                break; // we don't support multiple locally changed files per stop
            }
        }
    }

    @Override
    public Map<IRevisionedFile, IRevisionedFile> getHistory() {
        return Collections.unmodifiableMap(this.historyOrder);
    }

    @Override
    public IMultimap<IRevisionedFile, IHunk> getHunks() {
        return this.history.readOnlyView();
    }

    @Override
    public List<IHunk> getContentFor(final IRevisionedFile revision) {
        return this.history.get(revision);
    }

    @Override
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
    public boolean canBeMergedWith(final IStop other) {
        if (!this.mostRecentFile.equals(other.getOriginalMostRecentFile())) {
            return false;
        }
        if (this.mostRecentFragment == null) {
            if (other.getOriginalMostRecentFragment() == null) {
                return true;
            } else {
                return false;
            }
        } else {
            if (other.getOriginalMostRecentFragment() == null) {
                return false;
            } else {
                final boolean fragmentsMergeable =
                        this.mostRecentFragment.canBeMergedWith(other.getOriginalMostRecentFragment());
                if (this.irrelevantForReview == other.isIrrelevantForReview()) {
                    return fragmentsMergeable;
                } else {
                    return fragmentsMergeable
                            && !this.mostRecentFragment.isNeighboring(other.getOriginalMostRecentFragment());
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

        final Multimap<IRevisionedFile, IHunk> mergedHistory = new Multimap<>();
        mergedHistory.putAll(this.history);
        mergedHistory.putAll(other.getHunks());
        mergedHistory.sortValues();

        return new Stop(
                this.wc,
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
        return this.getMostRecentFile().toLocalPath(this.wc).toFile().getAbsoluteFile();
    }

    /* (non-Javadoc)
     * @see de.setsoftware.reviewtool.model.changestructure.IStop#getNumberOfFragments()
     */
    @Override
    public int getNumberOfFragments() {
        int ret = 1;
        for (final Entry<IRevisionedFile, List<IHunk>> e : this.history.entrySet()) {
            ret += e.getValue().size();
        }
        return ret;
    }

    /* (non-Javadoc)
     * @see de.setsoftware.reviewtool.model.changestructure.IStop#getNumberOfAddedLines()
     */
    @Override
    public int getNumberOfAddedLines() {
        return this.mostRecentFragment == null ? 0 : this.mostRecentFragment.getNumberOfLines();
    }

    /* (non-Javadoc)
     * @see de.setsoftware.reviewtool.model.changestructure.IStop#getNumberOfRemovedLines()
     */
    @Override
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

    /* (non-Javadoc)
     * @see de.setsoftware.reviewtool.model.changestructure.IStop#isIrrelevantForReview()
     */
    @Override
    public boolean isIrrelevantForReview() {
        return this.irrelevantForReview;
    }

    @Override
    protected void fillStopsInto(final List<Stop> buffer) {
        buffer.add(this);
    }

}
