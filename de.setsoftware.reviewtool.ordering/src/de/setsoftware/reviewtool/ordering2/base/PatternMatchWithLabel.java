package de.setsoftware.reviewtool.ordering2.base;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternMatchWithLabel implements PatternMatch {

    private final Pattern pattern;
    private final String label;
    private final List<? extends Stop> stops;
    private final BitSet fixedPositions;

    public PatternMatchWithLabel(Pattern p, String label, List<? extends Stop> stops) {
        this.pattern = p;
        this.label = label;
        this.stops = stops;
        this.fixedPositions = new BitSet();
    }

    void fixStopAtPosition(int position) {
        this.fixedPositions.set(position);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.fixedPositions.hashCode();
        result = prime * result + this.label.hashCode();
        result = prime * result + this.pattern.hashCode();
        result = prime * result + this.stops.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PatternMatchWithLabel)) {
            return false;
        }
        final PatternMatchWithLabel other = (PatternMatchWithLabel) obj;
        return this.label.equals(other.label)
            && this.pattern.equals(other.pattern)
            && this.fixedPositions.equals(other.fixedPositions)
            && this.stops.equals(other.stops);
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder();
        ret.append(this.pattern);
        ret.append(",");
        ret.append(this.label);
        ret.append(": ");
        for (int i = 0; i < this.stops.size(); i++) {
            if (i > 0) {
                ret.append(", ");
            }
            if (this.fixedPositions.get(i)) {
                ret.append("*");
            }
            ret.append(this.stops.get(i));
        }
        return ret.toString();
    }

    @Override
    public boolean isContainedInOrder(List<Stop> permutation) {
        final Set<Stop> stopsToFind = new HashSet<Stop>(this.stops);
        final int startIndex = this.findFirstIndex(stopsToFind, permutation);
        int i = 0;
        while (!stopsToFind.isEmpty()) {
            final Stop s = permutation.get(startIndex + i);
            if (!stopsToFind.remove(s)) {
                return false;
            }
            if (this.fixedPositions.get(i)) {
                if (!this.stops.get(i).equals(s)) {
                    return false;
                }
            }
            i++;
        }
        return true;
    }

    @Override
    public boolean isContainedAdjacent(List<Stop> permutation) {
        final Set<Stop> stopsToFind = new HashSet<Stop>(this.stops);
        final int startIndex = this.findFirstIndex(stopsToFind, permutation);
        int i = 0;
        while (!stopsToFind.isEmpty()) {
            final Stop s = permutation.get(startIndex + i);
            if (!stopsToFind.remove(s)) {
                return false;
            }
            i++;
        }
        return true;
    }

    private int findFirstIndex(Set<Stop> stopsToFind, List<Stop> permutation) {
        for (int i = 0; i < permutation.size(); i++) {
            final Stop s = permutation.get(i);
            if (stopsToFind.contains(s)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Collection<? extends Stop> getStops() {
        return this.stops;
    }

}
