package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A part of a review tour, corresponding to some notion of "singular change".
 * It knows the file fragment it belongs to in the most current revision, but
 * also the changes that it is based on.
 */
public class Stop {

    private final List<FileInRevision> historyOrder = new ArrayList<>();
    private final Map<FileInRevision, List<Fragment>> history = new HashMap<>();

    private final FileInRevision mostRecentFile;
    private final Fragment mostRecentFragment;

    /**
     * Constructor for textual changes.
     */
    public Stop(Fragment from, Fragment to, Fragment traceFragment) {
        this.historyOrder.add(from.getFile());
        this.historyOrder.add(to.getFile());
        this.history.put(from.getFile(), Arrays.asList(from));
        this.history.put(to.getFile(), Arrays.asList(to));

        this.mostRecentFile = traceFragment.getFile();
        this.mostRecentFragment = traceFragment;
    }

    /**
     * Constructor for binary changes.
     */
    public Stop(FileInRevision from, FileInRevision to, FileInRevision traceFile) {
        this.historyOrder.add(from);
        this.historyOrder.add(to);

        this.mostRecentFile = traceFile;
        this.mostRecentFragment = null;
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

}
