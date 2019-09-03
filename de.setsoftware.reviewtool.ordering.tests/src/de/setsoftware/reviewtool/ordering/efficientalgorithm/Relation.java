package de.setsoftware.reviewtool.ordering.efficientalgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Defines a partial order relation like defined in the paper.
 */
public class Relation {

    /**
     * Helper class to keep track of the matches.
     */
    private static final class Matches {

        private final Map<MatchSet<String>, Set<StarMatchSet<String>>> matches = new HashMap<>();

        public void add(MatchSet<String> ms, Set<StarMatchSet<String>> satisfiedPositions) {
            final Set<StarMatchSet<String>> set = this.matches.get(ms);
            if (set == null) {
                this.matches.put(ms, satisfiedPositions);
            } else {
                set.addAll(satisfiedPositions);
            }
        }

        public void add(Matches matches) {
            for (final Entry<MatchSet<String>, Set<StarMatchSet<String>>> e : matches.matches.entrySet()) {
                this.add(e.getKey(), e.getValue());
            }
        }

        public boolean isBetterThanOrEqual(Matches other) {
            if (other.matches.keySet().equals(this.matches.keySet())) {
                for (final MatchSet<String> cur : this.matches.keySet()) {
                    final Set<StarMatchSet<String>> pr1 = this.matches.get(cur);
                    final Set<StarMatchSet<String>> pr2 = other.matches.get(cur);
                    if (!pr1.containsAll(pr2)) {
                        return false;
                    }
                }
                return true;
            } else if (this.matches.keySet().containsAll(other.matches.keySet())) {
                return true;
            } else {
                return false;
            }
        }

    }

    /**
     * Returns true iff the first order is at least as good as the second in terms of the "is better than" relation.
     */
    public static boolean isBetterThanOrEqual(List<String> order1, List<String> order2,
            List<MatchSet<String>> matchSets) {
        final Matches m1 = determineMatches(order1, matchSets);
        final Matches m2 = determineMatches(order2, matchSets);
        if (m1.isBetterThanOrEqual(m2)) {
            if (m2.isBetterThanOrEqual(m1)) {
                return order1.equals(order2);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static Matches determineMatches(
            List<String> order,
            List<MatchSet<String>> matchSets) {

        final Matches ret = new Matches();
        for (final MatchSet<String> ms : matchSets) {
            final List<String> matchOrder = matches(order, ms);
            if (matchOrder != null) {
                ret.add(ms, getSatisfiedPositions(matchOrder, ms));
                ret.add(determineMatches(
                        shrink(order, ms),
                        shrinkMs(matchSets, ms)));
            }
        }
        return ret;
    }

    private static List<String> shrink(List<String> order, MatchSet<String> ms) {
        final LinkedHashSet<String> ret = new LinkedHashSet<>();
        for (final String s : order) {
            ret.add(shrink(s, ms));
        }
        return new ArrayList<>(ret);
    }

    private static String shrink(final String s, MatchSet<String> ms) {
        if (ms.getChangeParts().contains(s)) {
            return createReplacement(ms);
        } else {
            return s;
        }
    }

    private static MatchSet<String> shrink(MatchSet<String> toShrink, MatchSet<String> ms) {
        final LinkedHashSet<String> ret = new LinkedHashSet<>();
        for (final String s : toShrink.getChangeParts()) {
            ret.add(shrink(s, ms));
        }
        if (toShrink instanceof StarMatchSet) {
            final StarMatchSet<String> star = (StarMatchSet<String>) toShrink;
            assert star.getDistinguishedPart().size() == 1 : "multi-core stars not supported here at the moment";
            final String center = star.getDistinguishedPart().iterator().next();
            return new StarMatchSet<>(Collections.singleton(shrink(center, ms)), ret);
        } else {
            return new UnorderedMatchSet<>(ret);
        }
    }

    private static List<MatchSet<String>> shrinkMs(List<MatchSet<String>> matchSets, MatchSet<String> ms) {
        final LinkedHashSet<MatchSet<String>> ret = new LinkedHashSet<>();
        for (final MatchSet<String> cur : matchSets) {
            final MatchSet<String> shrunk = shrink(cur, ms);
            if (shrunk.getChangeParts().size() > 1) {
                ret.add(shrunk);
            }
        }
        return new ArrayList<>(ret);
    }

    private static String createReplacement(MatchSet<String> ms) {
        final List<String> ret = new ArrayList<>(ms.getChangeParts());
        Collections.sort(ret);
        return ret.toString();
    }

    private static List<String> matches(List<String> cur, MatchSet<String> ms) {
        boolean foundStart = false;
        boolean foundEnd = false;
        final List<String> matchInOrder = new ArrayList<>();
        for (final String s : cur) {
            if (foundStart) {
                if (foundEnd) {
                    if (ms.getChangeParts().contains(s)) {
                        return null;
                    }
                } else {
                    if (!ms.getChangeParts().contains(s)) {
                        foundEnd = true;
                    } else {
                        matchInOrder.add(s);
                    }
                }
            } else {
                if (ms.getChangeParts().contains(s)) {
                    foundStart = true;
                    matchInOrder.add(s);
                }
            }
        }
        assert matchInOrder.size() == ms.getChangeParts().size();
        return matchInOrder;
    }

    private static Set<StarMatchSet<String>> getSatisfiedPositions(
            List<String> matchOrder,
            MatchSet<String> ms) {

        if (ms instanceof StarMatchSet) {
            final StarMatchSet<String> s = (StarMatchSet<String>) ms;
            final Set<String> dp = s.getDistinguishedPart();
            if (matchOrder.subList(0, dp.size()).containsAll(dp)) {
                return Collections.singleton(s);
            }
        }
        return Collections.emptySet();
    }

}
