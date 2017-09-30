package de.setsoftware.reviewtool.ordering2.base;

import java.util.HashSet;
import java.util.Set;

public class MatchSet {

    private final Set<PatternMatch> containedInOrder = new HashSet<>();
    private final Set<PatternMatch> containedAdjacent = new HashSet<>();

    void addContainedInOrder(PatternMatch m) {
        this.containedInOrder.add(m);
        this.addContainedAdjacent(m);
    }

    void addContainedAdjacent(PatternMatch m) {
        this.containedAdjacent.add(m);
    }

    public PartialCompareResult compareTo(MatchSet m2) {
        return this.combine(
                this.compareSets(this.containedInOrder, m2.containedInOrder),
                this.compareSets(this.containedAdjacent, m2.containedAdjacent));
    }

    private PartialCompareResult combine(PartialCompareResult r1, PartialCompareResult r2) {
        switch (r1) {
        case EQUAL:
            return r2;
        case GREATER:
            switch (r2) {
            case EQUAL:
            case GREATER:
                return PartialCompareResult.GREATER;
            case LESS:
            case INCOMPARABLE:
                return PartialCompareResult.INCOMPARABLE;
            }
        case LESS:
            switch (r2) {
            case EQUAL:
            case LESS:
                return PartialCompareResult.LESS;
            case GREATER:
            case INCOMPARABLE:
                return PartialCompareResult.INCOMPARABLE;
            }
        case INCOMPARABLE:
            return PartialCompareResult.INCOMPARABLE;
        }
        throw new AssertionError("should not happen");
    }

    private PartialCompareResult compareSets(Set<PatternMatch> s1, Set<PatternMatch> s2) {
        if (s1.containsAll(s2)) {
            if (s2.containsAll(s1)) {
                return PartialCompareResult.EQUAL;
            } else {
                return PartialCompareResult.GREATER;
            }
        } else {
            if (s2.containsAll(s1)) {
                return PartialCompareResult.LESS;
            } else {
                return PartialCompareResult.INCOMPARABLE;
            }
        }
    }

    @Override
    public String toString() {
        return this.containedInOrder + " " + this.containedAdjacent;
    }

}
