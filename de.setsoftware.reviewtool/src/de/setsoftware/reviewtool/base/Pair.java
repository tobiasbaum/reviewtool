package de.setsoftware.reviewtool.base;

/**
 * A simple pair of two values.
 * @param <T1> Type of the first value.
 * @param <T2> Type of the second value.
 */
public class Pair<T1, T2> {

    private final T1 v1;
    private final T2 v2;

    private Pair(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public T1 getFirst() {
        return this.v1;
    }

    public T2 getSecond() {
        return this.v2;
    }

    public static <P1,P2> Pair<P1, P2> create(P1 v1, P2 v2) {
        return new Pair<P1, P2>(v1, v2);
    }

}
