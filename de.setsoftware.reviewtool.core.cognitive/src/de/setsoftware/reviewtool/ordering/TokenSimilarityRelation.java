package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculator;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Groups stops that are similar to each other. Similarity is defined based on the
 * Jaccard similarity of the stops' token sets.
 */
public class TokenSimilarityRelation implements RelationMatcher {

    private static final double JACCARD_THRESHOLD = 0.7;

    @Override
    public Collection<? extends OrderingInfo> determineMatches(
            final List<ChangePart> changeParts, TourCalculatorControl control) throws InterruptedException {
        final List<Pair<ChangePart, Set<String>>> tokenSets = new ArrayList<>(changeParts.size());
        for (final ChangePart c : changeParts) {
            if (c.isFullyIrrelevantForReview()) {
                continue;
            }
            final Set<String> tokens = this.determineTokenSet(c);
            if (!tokens.isEmpty()) {
                tokenSets.add(Pair.create(c, tokens));
            }
        }

        TourCalculator.checkInterruption(control);

        final List<Pair<Double, OrderingInfo>> similarities = new ArrayList<>();
        for (int i = 0; i < tokenSets.size(); i++) {
            for (int j = i + 1; j < tokenSets.size(); j++) {
                final double sim = this.jaccardSimilarity(
                        tokenSets.get(i).getSecond(),
                        tokenSets.get(j).getSecond());
                if (sim > JACCARD_THRESHOLD) {
                    similarities.add(Pair.create(sim, OrderingInfoImpl.unordered(HierarchyExplicitness.NONE, null,
                            Arrays.asList(tokenSets.get(i).getFirst(), tokenSets.get(j).getFirst()))));
                }
            }
        }

        TourCalculator.checkInterruption(control);

        //order by similarity so that the most similar will be grouped first
        Collections.sort(similarities, new Comparator<Pair<Double, OrderingInfo>>() {
            @Override
            public int compare(
                    final Pair<Double, OrderingInfo> o1,
                    final Pair<Double, OrderingInfo> o2) {
                return Double.compare(o2.getFirst(), o1.getFirst());
            }
        });

        final List<OrderingInfo> ret = new ArrayList<>();
        for (final Pair<Double, OrderingInfo> p : similarities) {
            ret.add(p.getSecond());
        }
        return ret;
    }

    private Set<String> determineTokenSet(final ChangePart changePart) {
        final Set<String> tokens = new HashSet<>();
        for (final Stop s : changePart.getStops()) {
            final IFragment fragment = s.getOriginalMostRecentFragment();
            if (fragment == null) {
                continue;
            }

            this.parseTokens(tokens, fragment.getContent());

            //also consider the old content, so that code moves are regarded as similar
            //  not 100% accurate for complex structures, but hopefully sufficient
            final IRevisionedFile oldestFile = this.determineOldestFile(s);
            if (oldestFile != null) {
                for (final IHunk hunk : s.getContentFor(oldestFile)) {
                    this.parseTokens(tokens, hunk.getSource().getContent());
                }
            }
        }

        return tokens;
    }

    private IRevisionedFile determineOldestFile(final Stop s) {
        //determine one of oldest files; i.e. one which has no predecessor
        final Set<IRevisionedFile> toSet = new LinkedHashSet<>(s.getHistory().values());
        for (final IRevisionedFile candidate : s.getHistory().keySet()) {
            if (!toSet.contains(candidate)) {
                return candidate;
            }
        }
        //normally this should not happen, conservative implementation
        return null;
    }

    private void parseTokens(final Set<String> tokens, final String content) {
        final StringBuilder curToken = new StringBuilder();
        for (final char ch : content.toCharArray()) {
            if (Character.isJavaIdentifierPart(ch)) {
                curToken.append(ch);
            } else {
                if (curToken.length() > 0) {
                    tokens.add(curToken.toString());
                    curToken.setLength(0);
                }
            }
        }
        if (curToken.length() > 0) {
            tokens.add(curToken.toString());
        }
    }

    private double jaccardSimilarity(final Set<String> s1, final Set<String> s2) {
        if (s2.size() < s1.size()) {
            //performance optimization
            return this.jaccardSimilarity(s2, s1);
        }
        int intersectionSize = 0;
        int unionSize = s1.size() + s2.size();
        for (final String s : s1) {
            if (s2.contains(s)) {
                intersectionSize++;
                unionSize--;
            }
        }
        return ((double) intersectionSize) / unionSize;
    }

}
