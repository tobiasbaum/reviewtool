package de.setsoftware.reviewtool.model.api;

import java.util.Collection;
import java.util.Set;

/**
 * A fragment is the smallest unit of a change. A fragment is generally checked as a whole by a reviewer,
 * and separately from other fragments. Examples for fragments are methods that are new or changed considerably,
 * but also single lines or even parts of expressions, if there was only a small change in that area of the code.
 * A fragment denotes a continuous portion of a file in a specific revision. If the change for this fragment was
 * a pure deletion, so that there is no code to point to left in that revision of the file, this is denoted
 * specially.
 */
public interface IFragment extends Comparable<IFragment> {

    /**
     * Returns the {@link IRevisionedFile} this {@link IFragment} is part of.
     */
    public abstract IRevisionedFile getFile();

    /**
     * The start position of the fragment (inclusive).
     */
    public abstract IPositionInText getFrom();

    /**
     * The end position of the fragment (exclusive).
     */
    public abstract IPositionInText getTo();

    /**
     * @return {@code true} if this is an in-line fragment.
     */
    public abstract boolean isInline();

    /**
     * Returns the size of this fragment.
     */
    public abstract IDelta getSize();

    /**
     * Returns an unmodifiable set of all original fragments contributing to this fragment.
     */
    public abstract Set<? extends IFragment> getOrigins();

    /**
     * Returns the content lines underlying this fragment.
     * Full lines are returned, even if the fragment spans only part of the line(s).
     */
    public abstract String getContentFullLines();

    /**
     * Returns the content underlying this fragment.
     * Only the content inside the fragments region is returned, i.e. the start and end might not be a full line.
     */
    public abstract String getContent();

    /**
     * @return {@code true} if this fragment is an original one, i.e. if it has no other origin(s).
     */
    public abstract boolean isOrigin();

    /**
     * Returns true iff this fragment is a direct neighbor of the given other fragment, but
     * does not overlap with it.
     */
    public abstract boolean isNeighboring(IFragment other);

    /**
     * Returns true if this fragment overlaps the passed one. Adjacent fragments do not overlap. Because of this,
     * deletion fragments starting at the same line do not overlap either as they are taken to be adjacent.
     * @param other The other fragment.
     * @return True if overlapping has been detected, else false.
     */
    public abstract boolean overlaps(IFragment other);

    /**
     * Returns true if this fragment is adjacent to the passed one. Adjacent fragments do not overlap.
     * @param other The other fragment.
     * @return True if fragments are adjacent, else false.
     */
    public abstract boolean isAdjacentTo(IFragment other);

    /**
     * Returns {@code true} if this fragment originates from some fragment which overlaps or is adjacent to
     * at least one of the fragments passed.
     */
    public abstract boolean containsChangeInOneOf(Collection<? extends IFragment> fragments);

    /**
     * Adjoins two adjacent fragments. The associated IRevisionedFile of the resulting fragment is taken from this
     * fragment.
     * @param other The other fragment.
     * @return The adjoint fragment encompassing both original fragments.
     */
    public abstract IFragment adjoin(IFragment other);

    /**
     * Subtracts some fragment from this fragment.
     * @param other The other fragment.
     * @return A list of remaining fragments. It may be empty or contain one or two fragments.
     */
    public abstract IFragmentList subtract(IFragment other);

    /**
     * Subtracts some fragment from this fragment.
     * @param other The other fragment.
     * @return A list of remaining fragments. It may be empty or contain one or two fragments.
     */
    public abstract IFragmentList subtract(IFragmentList other);

    /**
     * Returns true iff this fragment can be merged with the given fragment
     * into a (potentially larger) continuous fragment.
     */
    public abstract boolean canBeMergedWith(IFragment other);

    /**
     * Creates a new fragment combining the area in the file spanned by this
     * and the given other fragment.
     */
    public abstract IFragment merge(IFragment other);

    /**
     * @return {@code true} if this fragment is empty, else {@code false}.
     */
    public abstract boolean isDeletion();

    /**
     * Returns the number of lines in this fragment. For deletion fragments, zero is returned.
     * A line that is not contained up to its end is not counted. As a special case, this means
     * that for changes inside a single line, zero is returned.
     */
    public abstract int getNumberOfLines();

    /**
     * Creates a new fragment whose start and end positions are shifted by the given delta.
     * Only if this is an in-line fragment, the delta's column offset is applied to this fragment's end position.
     * @param delta The delta to add.
     * @return The resulting fragment.
     */
    public abstract IFragment adjust(IDelta delta);

    /**
     * Creates a fragment whose file is set to the one passed.
     * @param newFile The {@link IRevisionedFile} to use.
     * @return The resulting fragment.
     */
    public abstract IFragment setFile(IRevisionedFile newFile);

}