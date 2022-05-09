package de.setsoftware.reviewtool.base;

import java.io.Serializable;

/**
 * Wraps a {@link Comparable} in an object that supports the {@link IPartiallyComparable} interface.
 *
 * @param <T> The type of the underlying {@link Comparable}.
 */
public final class ComparableWrapper<T extends Comparable<T>>
        implements IPartiallyComparable<ComparableWrapper<T>>, Serializable {

    private static final long serialVersionUID = -167944665788166042L;
    private final T wrappedComparable;

    private ComparableWrapper(final T wrappedComparable) {
        this.wrappedComparable = wrappedComparable;
    }

    /**
     * Returns the wrapped {@link Comparable}.
     */
    public T getWrappedComparable() {
        return this.wrappedComparable;
    }

    /**
     * Wraps a {@link Comparable}.
     * @param o The {@link Comparable} to be wrapped.
     */
    public static <T extends Comparable<T>> ComparableWrapper<T> wrap(final T o) {
        return new ComparableWrapper<>(o);
    }

    /**
     * Unwraps a {@link IPartiallyComparable} provided it is a {@link ComparableWrapper}.
     * @param o The {@link ComparableWrapper} to be unwrapped.
     * @throws ClassCastException if {@code o} is not a {@link ComparableWrapper}.
     */
    public static <T extends Comparable<T>> T unwrap(final IPartiallyComparable<?> o) {
        @SuppressWarnings("unchecked")
        final ComparableWrapper<T> comparable = (ComparableWrapper<T>) (o);
        return comparable.wrappedComparable;
    }

    @Override
    public boolean le(final ComparableWrapper<T> other) {
        return this.wrappedComparable.compareTo(other.wrappedComparable) <= 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ComparableWrapper) {
            final ComparableWrapper<?> other = (ComparableWrapper<?>) (obj);
            return this.wrappedComparable.equals(other.wrappedComparable);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.wrappedComparable.hashCode();
    }

    @Override
    public String toString() {
        return this.wrappedComparable.toString();
    }
}
