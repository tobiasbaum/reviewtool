package de.setsoftware.reviewtool.model.changestructure;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * A fragment is the smallest unit of a change. A fragment is generally checked as a whole by a reviewer,
 * and separately from other fragments. Examples for fragments are methods that are new or changed considerably,
 * but also single lines or even parts of expressions, if there was only a small change in that area of the code.
 * A fragment denotes a continuous portion of a file in a specific revision. If the change for this fragment was
 * a pure deletion, so that there is no code to point to left in that revision of the file, this is denoted
 * specially.
 */
public final class Fragment implements Comparable<Fragment> {

    private final FileInRevision file;
    private final PositionInText from;
    private final PositionInText to;
    private final Set<Fragment> origins;
    private String content;

    Fragment(final FileInRevision file, final PositionInText from, final PositionInText to,
            final Fragment... origins) {
        this(file, from, to, Arrays.asList(origins));
    }

    Fragment(final FileInRevision file, final PositionInText from, final PositionInText to,
            final Collection<Fragment> origins) {
        this(file, from, to, combineOrigins(origins));
    }

    private Fragment(final FileInRevision file, final PositionInText from, final PositionInText to,
            final Set<Fragment> origins) {
        assert file != null;
        assert from != null;
        assert to != null;
        this.file = file;
        this.from = from;
        this.to = to;

        // if the set of origins contains only one Fragment equal to this one, we omit it
        this.origins = new LinkedHashSet<>();
        if (origins.size() != 1 || !origins.iterator().next().equals(this)) {
            this.origins.addAll(origins);
        }
    }

    /**
     * Combines the origins into a single set.
     * @param origins A collection of fragments.
     * @return The resulting set.
     */
    private static Set<Fragment> combineOrigins(final Collection<Fragment> origins) {
        final Set<Fragment> newOrigins = new LinkedHashSet<>();
        for (final Fragment origin : origins) {
            newOrigins.addAll(origin.getOrigins());
        }
        return newOrigins;
    }

    /**
     * Factory method that creates a fragment with already set content string.
     * Mainly for unit tests. Normally, fragments should be created using
     * the {@link ChangestructureFactory}.
     */
    public static Fragment createWithContent(
            FileInRevision file, PositionInText from, PositionInText to, String content) {
        final Fragment ret = new Fragment(file, from, to);
        ret.content = content;
        return ret;
    }

    public FileInRevision getFile() {
        return this.file;
    }

    /**
     * The start position of the fragment (inclusive).
     */
    public PositionInText getFrom() {
        return this.from;
    }

    /**
     * The end position of the fragment (inclusive).
     */
    public PositionInText getTo() {
        return this.to;
    }

    /**
     * @return {@code true} if this is an in-line fragment.
     */
    public boolean isInline() {
        return this.from.getLine() == this.to.getLine();
    }

    /**
     * Returns the size of this fragment.
     */
    public Delta getSize() {
        return this.to.minus(this.from);
    }

    /**
     * Returns an unmodifiable set of all original fragments contributing to this fragment.
     */
    public Set<Fragment> getOrigins() {
        if (this.isOrigin()) {
            final Set<Fragment> result = new LinkedHashSet<>();
            result.add(this);
            return Collections.unmodifiableSet(result);
        } else {
            return Collections.unmodifiableSet(this.origins);
        }
    }

    /**
     * Returns the content lines underlying this fragment.
     * Full lines are returned, even if the fragment spans only part of the line(s).
     */
    public String getContentFullLines() {
        if (this.content == null) {
            this.content = this.extractContent();
        }
        return this.content;
    }

    private String extractContent() {
        if (this.isDeletion()) {
            return "";
        }

        final byte[] contents;
        try {
            contents = this.file.getContents();
        } catch (final Exception e) {
            return "?";
        }

        try {
            final BufferedReader r = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(contents), "UTF-8"));
            final StringBuilder ret = new StringBuilder();
            int lineNumber = 1;
            String lineContent;
            while ((lineContent = r.readLine()) != null) {
                if (lineNumber >= this.from.getLine()) {
                    if (lineNumber < this.to.getLine()) {
                        ret.append(lineContent).append('\n');
                    } else if (lineNumber == this.to.getLine()) {
                        if (this.to.getColumn() > 1) {
                            ret.append(lineContent).append('\n');
                        }
                    }
                }
                lineNumber++;
            }
            return ret.toString();
        } catch (final IOException e) {
            throw new AssertionError("unexpected exception", e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(this.from.toString());
        result.append(" - ");
        result.append(this.to.toString());
        result.append(" in ");
        result.append(this.file.toString());
        result.append(this.origins.toString());
        return result.toString();
    }

    /**
     * @return {@code true} if this fragment is an original one, i.e. if it has no other origin(s).
     */
    public boolean isOrigin() {
        return this.origins.isEmpty();
    }

    /**
     * Returns true iff this fragment is a direct neighbor of the given other fragment, but
     * does not overlap with it.
     */
    public boolean isNeighboring(Fragment other) {
        if (!this.file.equals(other.file)) {
            return false;
        }
        return this.isAdjacentTo(other);
    }

    /**
     * Returns true if this fragment overlaps the passed one. Adjacent fragments do not overlap. Because of this,
     * deletion fragments starting at the same line do not overlap either as they are taken to be adjacent.
     * @param other The other fragment.
     * @return True if overlapping has been detected, else false.
     */
    public boolean overlaps(final Fragment other) {
        return this.to.compareTo(other.from) > 0 && this.from.compareTo(other.to) < 0;
    }

    /**
     * Returns true if this fragment is adjacent to the passed one. Adjacent fragments do not overlap.
     * @param other The other fragment.
     * @return True if fragments are adjacent, else false.
     */
    public boolean isAdjacentTo(final Fragment other) {
        return this.to.equals(other.from) || this.from.equals(other.to);
    }

    /**
     * Returns {@code true} if this fragment originates from some fragment which overlaps or is adjacent to
     * at least one of the fragments passed.
     */
    public boolean containsChangeInOneOf(final Collection<Fragment> fragments) {
        for (final Fragment origin : this.getOrigins()) {
            for (final Fragment fragment : fragments) {
                if (origin.overlaps(fragment) || origin.isAdjacentTo(fragment)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adjoins two adjacent fragments. The associated FileInRevision of the resulting fragment is taken from this
     * fragment.
     * @param other The other fragment.
     * @return The adjoint fragment encompassing both original fragments.
     */
    public Fragment adjoin(final Fragment other) {
        assert this.isAdjacentTo(other);
        if (this.to.equals(other.from)) {
            return new Fragment(this.file, this.from, other.to, this, other);
        } else {
            return new Fragment(this.file, other.from, this.to, this, other);
        }
    }

    /**
     * Subtracts some fragment from this fragment.
     * @param other The other fragment.
     * @return A list of remaining fragments. It may be empty or contain one or two fragments.
     */
    public FragmentList subtract(final Fragment other) {
        if (!this.overlaps(other)) {
            return new FragmentList(this);
        } else {
            try {
                final FragmentList fragmentList = new FragmentList();
                if (this.from.lessThan(other.from)) {
                    fragmentList.addFragment(new Fragment(this.file, this.from, other.from, this));
                }
                if (other.to.lessThan(this.to)) {
                    fragmentList.addFragment(new Fragment(this.file, other.to, this.to, this));
                }
                return fragmentList;
            } catch (final IncompatibleFragmentException e) {
                throw new ReviewtoolException(e);
            }
        }
    }

    /**
     * Subtracts some fragment from this fragment.
     * @param other The other fragment.
     * @return A list of remaining fragments. It may be empty or contain one or two fragments.
     */
    public FragmentList subtract(final FragmentList other) {
        return new FragmentList(this).subtract(other);
    }

    /**
     * Returns true iff this fragment can be merged with the given fragment
     * into a (potentially larger) continuous fragment.
     */
    public boolean canBeMergedWith(Fragment other) {
        if (!this.file.equals(other.file)) {
            return false;
        }
        return this.isAdjacentTo(other) || this.overlaps(other);
    }

    /**
     * Creates a new fragment combining the area in the file spanned by this
     * and the given other fragment.
     */
    public Fragment merge(Fragment other) {
        if (other.getFrom().lessThan(this.getFrom())) {
            return other.merge(this);
        }

        assert this.canBeMergedWith(other);
        final PositionInText minFrom = this.getFrom();
        final PositionInText maxTo;
        if (this.to.lessThan(other.to)) {
            maxTo = other.to;
        } else {
            maxTo = this.to;
        }
        return new Fragment(this.file, minFrom, maxTo, this, other);
    }

    public boolean isDeletion() {
        return this.to.equals(this.from);
    }

    /**
     * Creates a fragment whose file is set to the one passed.
     * @param newFile The {@link FileInRevision} to use.
     * @return The resulting fragment.
     */
    Fragment setFile(final FileInRevision newFile) {
        return new Fragment(newFile, this.from, this.to, this);
    }

    @Override
    public int hashCode() {
        return this.from.hashCode() + 31 * this.file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Fragment)) {
            return false;
        }
        final Fragment other = (Fragment) obj;
        return this.file.equals(other.file)
            && this.from.equals(other.from)
            && this.to.equals(other.to)
            && this.origins.equals(other.origins);
    }

    /**
     * Returns the number of lines in this fragment. For deletion fragments, zero is returned.
     * A line that is not contained up to its end is not counted. As a special case, this means
     * that for changes inside a single line, zero is returned.
     */
    public int getNumberOfLines() {
        return this.to.getLine() - this.from.getLine();
    }

    /**
     * Creates a new fragment whose start and end positions are shifted by the given delta.
     * Only if this is an in-line fragment, the delta's column offset is applied to this fragment's end position.
     * @param delta The delta to add.
     * @return The resulting fragment.
     */
    public Fragment adjust(final Delta delta) {
        return new Fragment(
                this.file,
                this.from.plus(delta),
                this.to.plus(this.isInline() ? delta : delta.ignoreColumnOffset()),
                this);
    }

    @Override
    public int compareTo(final Fragment o) {
        final int from = this.getFrom().compareTo(o.getFrom());
        return from != 0 ? from : this.getTo().compareTo(o.getTo());
    }

}
