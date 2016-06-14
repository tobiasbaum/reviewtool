package de.setsoftware.reviewtool.base;

/**
 * Some utility functions.
 */
public class Util {

    /**
     * Returns true iff o1 equals o2.
     * Both arguments can be null.
     */
    public static boolean sameOrEquals(Object o1, Object o2) {
        return o1 == o2 || (o1 != null && o1.equals(o2));
    }

}
