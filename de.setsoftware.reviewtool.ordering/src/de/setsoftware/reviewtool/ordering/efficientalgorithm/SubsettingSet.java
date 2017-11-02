package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * An implementation of SimpleSet that allows efficient stepwise selection
 * of a subset of MatchSets.
 *
 * @param <S> Type of the items.
 */
public class SubsettingSet<S> implements SimpleSet<S> {

    private final List<MatchSet<S>> potentialFolds;
    private final Set<Integer> remainingFoldIndices;

    private final CountingSet<S> allItems;
    private final CountingSet<S> removedItems;
    private Integer currentCandidate;

    public SubsettingSet(MatchSet<S> toMatch, List<MatchSet<S>> potentialFolds) {
        this.potentialFolds = new ArrayList<>(potentialFolds);
        this.remainingFoldIndices = new TreeSet<>();
        for (int i = 0; i < potentialFolds.size(); i++) {
            this.remainingFoldIndices.add(i);
        }

        this.allItems = new CountingSet<>(toMatch.getChangeParts().size() + potentialFolds.size() * 4);
        this.allItems.addAll(toMatch.getChangeParts());
        for (final MatchSet<S> ms : potentialFolds) {
            this.allItems.addAll(ms.getChangeParts());
        }
        this.removedItems = new CountingSet<>();
    }

    @Override
    public boolean contains(S item) {
        final int addCount = this.allItems.get(item);
        int removeCount = this.removedItems.get(item);
        if (this.currentCandidate != null
                && this.getCurrentCandidate().getChangeParts().contains(item)) {
            removeCount++;
        }
        return addCount > removeCount;
    }

    @Override
    public Set<S> toSet() {
        final Set<S> ret = new LinkedHashSet<>();
        for (final S item : this.allItems.keys()) {
            if (this.contains(item)) {
                ret.add(item);
            }
        }
        return ret;
    }

    private MatchSet<S> getCurrentCandidate() {
        return this.potentialFolds.get(this.currentCandidate);
    }

    /**
     * Returns the removals that are still possible.
     */
    public Collection<Integer> potentialRemovals() {
        assert this.currentCandidate == null;
        return new ArrayList<>(this.remainingFoldIndices);
    }

    /**
     * Removes the MatchSet with the given index. The removal is only preliminary and
     * has to be either committed or rolled back in the next step.
     */
    public void preliminaryRemove(Integer foldKey) {
        assert this.currentCandidate == null;
        this.currentCandidate = foldKey;
    }

    /**
     * Commits the most recent preliminary removal, so that a new removal is possible.
     */
    public void commitRemoval() {
        this.removedItems.addAll(this.getCurrentCandidate().getChangeParts());
        this.remainingFoldIndices.remove(this.currentCandidate);
        this.currentCandidate = null;
    }

    /**
     * Undoes to most recent preliminary removal.
     */
    public void rollbackRemoval() {
        this.currentCandidate = null;
    }

}
