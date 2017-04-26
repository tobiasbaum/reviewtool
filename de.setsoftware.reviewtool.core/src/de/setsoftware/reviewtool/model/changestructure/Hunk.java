package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

/**
 * Encapsulates a single difference between two revisions of a file, i.e. a pair (source fragment, target fragment).
 * <p/>
 * Hunks can be ordered according to their source fragment.
 */
public final class Hunk implements Comparable<Hunk> {

    /**
     * The source fragment.
     */
    private final Fragment source;
    /**
     * The target fragment.
     */
    private final Fragment target;
    /**
     * The hunk delta (number of lines added minus number of lines deleted).
     */
    private final Delta delta;

    /**
     * Creates a Hunk.
     * @param source The source fragment.
     * @param target The target fragment.
     */
    public Hunk(final Fragment source, final Fragment target) {
        this.source = source;
        this.target = target;
        this.delta = this.target.getSize().minus(this.source.getSize());
    }

    /**
     * Creates a Hunk from a {@link TextualChangeHunk}.
     * @param source The source fragment.
     * @param target The target fragment.
     */
    public Hunk(final TextualChangeHunk hunk) {
        this(hunk.getFromFragment(), hunk.getToFragment());
    }

    /**
     * @return The source fragment.
     */
    public Fragment getSource() {
        return this.source;
    }

    /**
     * @return The target fragment.
     */
    public Fragment getTarget() {
        return this.target;
    }

    /**
     * @return The hunk delta.
     */
    Delta getDelta() {
        return this.delta;
    }

    /**
     * @return {@code true} if this is an in-line hunk.
     */
    public boolean isInline() {
        return this.source.isInline();
    }

    /**
     * Combines all source fragments of a collection of hunks.
     * @param hunks The collection of hunks.
     * @return A FragmentList containing all source fragments of the hunks in order. Adjacent fragments are merged.
     */
    public static FragmentList getSources(final Collection<? extends Hunk> hunks) {
        final FragmentList result = new FragmentList();
        for (final Hunk hunk : hunks) {
            try {
                result.addFragment(hunk.getSource());
            } catch (final IncompatibleFragmentException e) {
                throw new Error(e);
            }
        }
        result.coalesce();
        return result;
    }

    /**
     * Combines all target fragments of a collection of hunks.
     * @param hunks The collection of hunks.
     * @return A FragmentList containing all target fragments of the hunks in order. Adjacent fragments are merged.
     */
    public static FragmentList getTargets(final Collection<? extends Hunk> hunks) {
        final FragmentList result = new FragmentList();
        for (final Hunk hunk : hunks) {
            try {
                result.addFragment(hunk.getTarget());
            } catch (final IncompatibleFragmentException e) {
                throw new Error(e);
            }
        }
        result.coalesce();
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof Hunk) {
            final Hunk hunk = (Hunk) other;
            return this.getClass() == other.getClass()
                && this.source.equals(hunk.getSource())
                && this.target.equals(hunk.getTarget());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.source.hashCode() ^ this.target.hashCode();
    }

    @Override
    public String toString() {
        return "Hunk: " + this.source + "(" + this.source.getContentFullLines() + ") --> "
                + this.target + "(" + this.target.getContentFullLines() + ")";
    }

    /**
     * Creates a new hunk whose source fragment's start and end positions are shifted by the given delta.
     * @param delta The delta to add.
     * @return The resulting hunk.
     */
    Hunk adjustSource(final Delta delta) {
        return new Hunk(this.getSource().adjust(delta), this.getTarget());
    }

    /**
     * Creates a new hunk whose target fragment's start and end positions are shifted by the given delta.
     * @param delta The delta to add.
     * @return The resulting hunk.
     */
    Hunk adjustTarget(final Delta delta) {
        return new Hunk(this.getSource(), this.getTarget().adjust(delta));
    }

    /**
     * Creates a new hunk whose source fragment's file is set to the one passed.
     * @param source The {@link FileInRevision} to use.
     * @return The resulting hunk.
     */
    Hunk adjustSourceFile(final FileInRevision source) {
        return new Hunk(this.source.setFile(source), this.target);
    }

    /**
     * Creates a new hunk whose target fragment's file is set to the one passed.
     * @param source The {@link FileInRevision} to use.
     * @return The resulting hunk.
     */
    Hunk adjustTargetFile(final FileInRevision target) {
        return new Hunk(this.source, this.target.setFile(target));
    }

    @Override
    public int compareTo(final Hunk o) {
        return this.source.compareTo(o.getSource());
    }

}
