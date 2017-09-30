package de.setsoftware.reviewtool.ordering2.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class CompositeStop implements Stop {

    private final Set<Stop> children;

    public CompositeStop(Collection<? extends Stop> children) {
        this.children = new LinkedHashSet<>();
        for (final Stop s : children) {
            if (s instanceof CompositeStop) {
                this.children.addAll(((CompositeStop) s).children);
            } else {
                this.children.add(s);
            }
        }
    }

    @Override
    public int hashCode() {
        return this.children.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompositeStop)) {
            return false;
        }
        return ((CompositeStop) o).children.equals(this.children);
    }

    @Override
    public String toString() {
        return this.children.toString();
    }

    @Override
    public int compareTo(Stop o) {
        if (o instanceof CompositeStop) {
            final CompositeStop c = (CompositeStop) o;
            final int cmpSize = Integer.compare(this.children.size(), c.children.size());
            if (cmpSize != 0) {
                return cmpSize;
            }
            final Iterator<? extends Stop> iter1 = this.children.iterator();
            final Iterator<? extends Stop> iter2 = c.children.iterator();
            while(iter1.hasNext()) {
                final int cmp = iter1.next().compareTo(iter2.next());
                if (cmp != 0) {
                    return cmp;
                }
            }
            return 0;
        } else {
            return this.getClass().toString().compareTo(o.getClass().toString());
        }
    }

}
