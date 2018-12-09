package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Contains the algorithm for efficiently grouping and reordering change parts/stops.
 * Internally, it uses two types of trees, one for the grouping and one for re-ordering based on the results
 * of the grouping.
 *
 * @param <T> Type of the stops.
 */
public class TourCalculator<T> {

    private final Set<MatchSet<T>> successfulMatches = new LinkedHashSet<>();
    private List<T> resultingTour;

    /**
     * Helper class for recursive folding and matching.
     *
     * @param <S> Type of the stops.
     */
    private static final class FoldMatchingHelper<S> {

        private BundleCombinationTreeElement<S> bundler;
        private final Map<MatchSet<S>, List<MatchSet<S>>> unsatisfiedMatchesWithPotentiallyRelevantFolds;
        private final LinkedList<MatchSet<S>> todoQueue;
        private final Map<MatchSet<S>, List<MatchSet<S>>> matchedWithFolds;

        public FoldMatchingHelper(BundleCombinationTreeElement<S> bundler, List<MatchSet<S>> unsatisfiedMatches) {
            this.bundler = bundler;
            this.unsatisfiedMatchesWithPotentiallyRelevantFolds = new LinkedHashMap<>();
            for (final MatchSet<S> unsatisfiedMatch : unsatisfiedMatches) {
                this.unsatisfiedMatchesWithPotentiallyRelevantFolds.put(unsatisfiedMatch, new ArrayList<MatchSet<S>>());
            }
            this.todoQueue = new LinkedList<>();
            this.matchedWithFolds = new LinkedHashMap<>();
        }

        public void addPotentialFolds(Collection<MatchSet<S>> matches, TourCalculatorControl control)
            throws InterruptedException {

            this.todoQueue.addAll(matches);
            while (!this.todoQueue.isEmpty()) {
                //assign the next batch of folds to try to the unsatisfied matches they might help to satisfy
                final Set<MatchSet<S>> unsatisfiedMatchesThatCouldNowMatch = new LinkedHashSet<>();
                while (!this.todoQueue.isEmpty()) {
                    final MatchSet<S> toFold = this.todoQueue.poll();
                    final Iterator<Entry<MatchSet<S>, List<MatchSet<S>>>> iter =
                            this.unsatisfiedMatchesWithPotentiallyRelevantFolds.entrySet().iterator();
                    while (iter.hasNext()) {
                        final Entry<MatchSet<S>, List<MatchSet<S>>> e = iter.next();
                        //to be able to satisfy the match, the candidate fold has to contain elements from the match
                        if (!disjoint(e.getKey().getChangeParts(), toFold.getChangeParts())) {
                            this.removeSubsets(e.getValue(), toFold);
                            e.getValue().add(toFold);
                            unsatisfiedMatchesThatCouldNowMatch.add(e.getKey());
                        }
                    }
                }

                checkInterruption(control);

                //check for matches that can now be satisfied
                for (final MatchSet<S> toMatch : unsatisfiedMatchesThatCouldNowMatch) {
                    this.matchWithNewFold(
                            toMatch,
                            this.unsatisfiedMatchesWithPotentiallyRelevantFolds.get(toMatch),
                            control);
                }
            }
        }

        private void removeSubsets(List<MatchSet<S>> value, MatchSet<S> toFold) {
            final Iterator<MatchSet<S>> iter = value.iterator();
            while (iter.hasNext()) {
                final MatchSet<S> cur = iter.next();
                if (toFold.getChangeParts().containsAll(cur.getChangeParts())) {
                    iter.remove();
                }
            }
        }

        private void matchWithNewFold(
                MatchSet<S> toMatch, List<MatchSet<S>> potentialFolds, TourCalculatorControl control) {
            final SubsettingSet<S> activeFolds = new SubsettingSet<>(toMatch, potentialFolds);
            final boolean matchesWithFullSet = this.matchesWithFoldSubset(toMatch, activeFolds);
            if (!matchesWithFullSet) {
                //does not match with the full set, cannot match with a subset either
                return;
            }

            //determine a minimal subset that still allows the match to happen
            //when the calculation already took quite long, don't go for minimality
            if (!control.isFastModeNeeded()) {
                for (final Integer index : activeFolds.potentialRemovals()) {
                    //try without a fold
                    activeFolds.preliminaryRemove(index);
                    if (this.matchesWithFoldSubset(toMatch, activeFolds)) {
                        //still matches => fold is unnecessary
                        activeFolds.commitRemoval();
                    } else {
                        //does not match any more => fold is necessary
                        activeFolds.rollbackRemoval();
                    }
                }
            }

            //change attributes according to match
            this.bundler = this.bundler.bundle(activeFolds);
            this.todoQueue.add(new MatchSet<>(activeFolds.toSet()));
            this.matchedWithFolds.put(toMatch, this.selectActiveFolds(potentialFolds, activeFolds));
            this.unsatisfiedMatchesWithPotentiallyRelevantFolds.remove(toMatch);
        }

        private boolean matchesWithFoldSubset(@SuppressWarnings("unused") MatchSet<S> toMatch, SimpleSet<S> set) {
            return this.bundler.bundle(set) != null;
        }

        private List<MatchSet<S>> selectActiveFolds(
                List<MatchSet<S>> potentialFolds, SubsettingSet<S> activeFolds) {

            final List<MatchSet<S>> ret = new ArrayList<>();
            for (final Integer index : activeFolds.potentialRemovals()) {
                ret.add(potentialFolds.get(index));
            }
            return ret;
        }

        public BundleCombinationTreeElement<S> getBundler() {
            return this.bundler;
        }

    }

    private static<S> boolean disjoint(Set<S> c1, Set<S> c2) {
        Set<S> iterate;
        Set<S> contains;
        if (c1.size() > c2.size()) {
            iterate = c2;
            contains = c1;
        } else {
            iterate = c1;
            contains = c2;
        }

        for (final S e : iterate) {
            if (contains.contains(e)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine a reordering that satisfies as many of the given grouping and positioning requests as possible.
     * A resulting tour can be requested from the returned {@link TourCalculator} object.
     *
     * @param allChangeParts The change parts/stops to reorder.
     * @param matchSets The match sets to group. Order in list determines priority.
     * @param positionRequests The position requests to satisfy. Order in list determines priority.
     */
    public static<S> TourCalculator<S> calculateFor(
            List<S> allChangeParts,
            List<MatchSet<S>> matchSets,
            List<PositionRequest<S>> positionRequests,
            Comparator<S> tieBreakingComparator,
            TourCalculatorControl isCanceled) throws InterruptedException {
        assert new HashSet<>(allChangeParts).size() == allChangeParts.size() : "there are duplicate change parts";

        final TourCalculator<S> ret = new TourCalculator<>();
        if (allChangeParts.size() <= 1) {
            ret.resultingTour = new ArrayList<>(allChangeParts);
            return ret;
        }

        //phase one: bundling without folding
        BundleCombinationTreeElement<S> bundler = BundleCombinationTreeElement.create(allChangeParts);
        final List<MatchSet<S>> unsatisfiedMatches = new ArrayList<>();
        for (final MatchSet<S> matchSet : matchSets) {
            final BundleCombinationTreeElement<S> next =
                    bundler.bundle(new SimpleSetAdapter<>(matchSet.getChangeParts()));
            if (next != null) {
                bundler = next;
                ret.successfulMatches.add(matchSet);
            } else {
                unsatisfiedMatches.add(matchSet);
            }
            checkInterruption(isCanceled);
        }

        if (isCanceled.isFastModeNeeded()) {
            //if the calculation already took too much time here, just give up with
            //  the results we have so far
            ret.resultingTour = bundler.getPossibleOrder(tieBreakingComparator);
            return ret;
        }

        //phase two: taking folds into account
        final FoldMatchingHelper<S> foldedBundler = new FoldMatchingHelper<>(bundler, unsatisfiedMatches);
        foldedBundler.addPotentialFolds(ret.successfulMatches, isCanceled);
        bundler = foldedBundler.getBundler();

        //phase three: try to put centers first for ordered match sets
        //TODO get rid of position requests
        final Map<MatchSet<S>, PositionRequest<S>> ordered = new HashMap<>();
        for (final PositionRequest<S> r : positionRequests) {
            ordered.put(r.getMatchSet(), r);
        }
        for (final MatchSet<S> matchSet : ret.successfulMatches) {
            if (!ordered.containsKey(matchSet)) {
                continue;
            }
            final PositionRequest<S> p = ordered.get(matchSet);
            final Set<S> rest = new HashSet<>(matchSet.getChangeParts());
            rest.remove(p.getDistinguishedPart());
            final BundleCombinationTreeElement<S> next = bundler.bundleOrdered(
                    new SimpleSetAdapter<>(Collections.singleton(p.getDistinguishedPart())),
                    new SimpleSetAdapter<>(rest));
            if (next != null) {
                bundler = next;
            }
            checkInterruption(isCanceled);
        }
        for (final MatchSet<S> matchSet : foldedBundler.matchedWithFolds.keySet()) {
            if (!ordered.containsKey(matchSet)) {
                continue;
            }
            final Set<S> center = new HashSet<>();
            center.add(ordered.get(matchSet).getDistinguishedPart());
            final Set<S> rest = new HashSet<>(matchSet.getChangeParts());
            for (final MatchSet<S> neededFold : foldedBundler.matchedWithFolds.get(matchSet)) {
                if (disjoint(neededFold.getChangeParts(), center)) {
                    rest.addAll(neededFold.getChangeParts());
                } else {
                    center.addAll(neededFold.getChangeParts());
                }
            }
            rest.removeAll(center);
            if (rest.isEmpty()) {
                continue;
            }
            final BundleCombinationTreeElement<S> next = bundler.bundleOrdered(
                    new SimpleSetAdapter<>(center),
                    new SimpleSetAdapter<>(rest));
            if (next != null) {
                bundler = next;
            }
            checkInterruption(isCanceled);
        }

        ret.resultingTour = bundler.getPossibleOrder(tieBreakingComparator);
        return ret;
    }

    public List<T> getTour() {
        return this.resultingTour;
    }

    /**
     * Helper method to check for the cancellation flag and throw an InterruptedException if needed.
     */
    public static void checkInterruption(TourCalculatorControl isCanceled) throws InterruptedException {
        if (Thread.interrupted() || isCanceled.isCanceled()) {
            //we use InterruptedException so that the sorting code is not depended on Eclipse
            //  to avoid surprises, we also check for Thread.interrupted()
            throw new InterruptedException();
        }
    }

}
