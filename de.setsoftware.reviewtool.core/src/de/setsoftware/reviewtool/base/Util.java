package de.setsoftware.reviewtool.base;

import java.util.Iterator;

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

    /**
     * Joins the string representation of all items in the collection with commas
     * and returns the resulting string.
     */
    public static String implode(Iterable<?> collection) {
        return implode(collection, ", ");
    }

    /**
     * Joins the string representation of all items in the collection with the given glue
     * and returns the resulting string.
     */
    public static String implode(Iterable<?> collection, String glue) {
        final Iterator<?> iter = collection.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        final StringBuilder ret = new StringBuilder();
        ret.append(iter.next());
        while (iter.hasNext()) {
            ret.append(glue).append(iter.next());
        }
        return ret.toString();
    }

}
