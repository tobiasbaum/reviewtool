package de.setsoftware.reviewtool.model.changestructure;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IClassification;

public class Classification implements IClassification {

    public static final IClassification[] NONE = new IClassification[0];

    private final String name;
    private final boolean mergeAsAnd;

    public Classification(String name, boolean mergeAsAnd) {
        this.name = name;
        this.mergeAsAnd = mergeAsAnd;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean mergeAsAnd() {
        return this.mergeAsAnd;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Classification)) {
            return false;
        }
        final Classification c = (Classification) o;
        return this.name.equals(c.name);
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Merges two sets of classifications.
     * Each classification type can specify if it is sufficient that he occurs in one change (or-type)
     * or if every change needs to be classified this way for it to occur in the merge (and-type).
     */
    public static IClassification[] merge(IClassification[] s1, IClassification[] s2) {
        final Set<IClassification> ret = new LinkedHashSet<>();
        for (final IClassification cl : s1) {
            if (cl.mergeAsAnd()) {
                if (Arrays.asList(s2).contains(cl)) {
                    ret.add(cl);
                }
            } else {
                ret.add(cl);
            }
        }
        for (final IClassification cl : s2) {
            if (!cl.mergeAsAnd()) {
                ret.add(cl);
            }
        }
        if (ret.isEmpty()) {
            return NONE;
        }
        return ret.toArray(new IClassification[ret.size()]);
    }

}
