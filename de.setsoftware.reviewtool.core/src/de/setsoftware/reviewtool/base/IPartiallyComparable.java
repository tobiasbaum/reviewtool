package de.setsoftware.reviewtool.base;

/**
 * Represents objects that can be partially ordered.
 *
 * <p>The partial order is defined solely by the operation {@link #le(IPartiallyComparable)}:
 * <ul>
 * <li>{@code a.le(b) && b.le(a)} => {@code a} and {@code b} are equal</li>
 * <li>{@code a.le(b) && !b.le(a)} => {@code a} comes before {@code b}</li>
 * <li>{@code !a.le(b) && b.le(a)} => {@code b} comes before {@code a}</li>
 * <li>{@code !a.le(b) && !b.le(a)} => {@code a} and {@code b} are incomparable</li>
 * </ul>
 *
 * @param <T> The concrete type of the objects.
 */
public interface IPartiallyComparable<T> {

    /**
     * Returns {@code true} iff this object is less than or equal to the passed one.
     */
    public abstract boolean le(final T other);
}
