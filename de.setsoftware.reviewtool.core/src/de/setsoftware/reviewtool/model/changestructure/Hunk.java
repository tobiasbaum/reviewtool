package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collection;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IDelta;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IFragmentList;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.api.IncompatibleFragmentException;

/**
 * Encapsulates a single difference between two revisions of a file, i.e. a pair (source fragment, target fragment).
 * <p/>
 * Hunks can be ordered according to their source fragment.
 */
public final class Hunk implements IHunk {

    private static final long serialVersionUID = 6069703227726679411L;

    /**
     * The source fragment.
     */
    private final IFragment source;
    /**
     * The target fragment.
     */
    private final IFragment target;
    /**
     * The hunk delta (number of lines added minus number of lines deleted).
     */
    private final IDelta delta;

    /**
     * Creates a Hunk.
     * @param source The source fragment.
     * @param target The target fragment.
     */
    public Hunk(final IFragment source, final IFragment target) {
        this.source = source;
        this.target = target;
        this.delta = this.target.getSize().minus(this.source.getSize());
    }

    /**
     * Creates a Hunk from a {@link TextualChangeHunk}.
     * @param source The source fragment.
     * @param target The target fragment.
     */
    public Hunk(final ITextualChange hunk) {
        this(hunk.getFromFragment(), hunk.getToFragment());
    }

    @Override
    public IFragment getSource() {
        return this.source;
    }

    @Override
    public IFragment getTarget() {
        return this.target;
    }

    @Override
    public IDelta getDelta() {
        return this.delta;
    }

    @Override
    public boolean isInline() {
        return this.source.isInline();
    }

    /**
     * Combines all source fragments of a collection of hunks.
     * @param hunks The collection of hunks.
     * @return A FragmentList containing all source fragments of the hunks in order. Adjacent fragments are merged.
     */
    public static IFragmentList getSources(final Collection<? extends IHunk> hunks) {
        final IFragmentList result = new FragmentList();
        for (final IHunk hunk : hunks) {
            try {
                result.addFragment(hunk.getSource());
            } catch (final IncompatibleFragmentException e) {
                throw new ReviewtoolException(e);
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
    public static IFragmentList getTargets(final Collection<? extends IHunk> hunks) {
        final IFragmentList result = new FragmentList();
        for (final IHunk hunk : hunks) {
            try {
                result.addFragment(hunk.getTarget());
            } catch (final IncompatibleFragmentException e) {
                throw new ReviewtoolException(e);
            }
        }
        result.coalesce();
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof Hunk) {
            final IHunk hunk = (IHunk) other;
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

    @Override
    public IHunk adjustSource(final IDelta delta) {
        return new Hunk(this.getSource().adjust(delta), this.getTarget());
    }

    @Override
    public IHunk adjustTarget(final IDelta delta) {
        return new Hunk(this.getSource(), this.getTarget().adjust(delta));
    }

    @Override
    public IHunk adjustSourceFile(final IRevisionedFile source) {
        return new Hunk(this.source.setFile(source), this.target);
    }

    @Override
    public IHunk adjustTargetFile(final IRevisionedFile target) {
        return new Hunk(this.source, this.target.setFile(target));
    }

    @Override
    public int compareTo(final IHunk o) {
        return this.source.compareTo(o.getSource());
    }

}
