package de.setsoftware.reviewtool.ordering.basealgorithm;

import java.util.List;

public class Tour<S> {

    private final List<S> stops;

    private Tour(List<S> asList) {
        this.stops = asList;
    }

    public static<S> Tour<S> of(List<S> ret) {
        return new Tour<S>(ret);
    }

    @Override
    public int hashCode() {
        return this.stops.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tour)) {
            return false;
        }
        final Tour<?> t = (Tour<?>) o;
        return t.stops.equals(this.stops);
    }

    @Override
    public String toString() {
        return this.stops.toString();
    }

    public List<S> getStops() {
        return this.stops;
    }

}
