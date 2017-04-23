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
    private final int delta;

    /**
     * Creates a Hunk.
     * @param source The source fragment.
     * @param target The target fragment.
     */
    public Hunk(final Fragment source, final Fragment target) {
        this.source = source;
        this.target = target;
        this.delta = this.target.getNumberOfLines() - this.source.getNumberOfLines();
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
     * @return The hunk delta, i.e. the number of lines added by this hunk minus number of lines deleted by this hunk.
     */
    int getDelta() {
        return this.delta;
    }

    /**
     * @return The delta in columns, when this is a single line change. Otherwise zero is returned.
     */
    int getColumnDelta() {
        if (this.source.getNumberOfLines() == 0 && this.target.getNumberOfLines() == 0) {
            final int colsTarget = this.target.getTo().getColumn() - this.target.getFrom().getColumn() + 1;
            final int colsSource = this.source.getTo().getColumn() - this.source.getFrom().getColumn() + 1;
            return colsTarget - colsSource;
        } else {
            return 0;
        }
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
     * Creates a new hunk whose source fragment's start and end positions are shifted by the given line offset.
     * @param offset The line offset to add.
     * @return The resulting hunk.
     */
    Hunk adjustSource(int offset) {
        return new Hunk(this.getSource().adjust(offset), this.getTarget());
    }

    /**
     * Creates a new hunk whose target fragment's start and end positions are shifted by the given line offset.
     * @param offset The line offset to add.
     * @return The resulting hunk.
     */
    Hunk adjustTarget(int offset) {
        return new Hunk(this.getSource(), this.getTarget().adjust(offset));
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

    /**
     * Returns the negative delta of this hunk if passed position is behind this hunk. This is helpful if some given
     * position has to be adjusted by "counting away" this hunk.
     * @param pos The position in question.
     * @return The line delta to be added to the position's line when this hunk is to be ignored.
     */
    int getLineDeltaBack(PositionInText pos) {
        if (pos.compareTo(this.target.getTo()) > 0) {
            return -this.getDelta();
        } else {
            return 0;
        }
    }

    @Override
    public int compareTo(final Hunk o) {
        return this.source.compareTo(o.getSource());
    }

}
