package de.setsoftware.reviewtool.model.changestructure;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A fragment is the smallest unit of a change. A fragment is generally checked as a whole by a reviewer,
 * and separately from other fragments. Examples for fragments are methods that are new or changed considerably,
 * but also single lines or even parts of expressions, if there was only a small change in that area of the code.
 * A fragment denotes a continuous portion of a file in a specific revision. If the change for this fragment was
 * a pure deletion, so that there is no code to point to left in that revision of the file, this is denoted
 * specially.
 */
public class Fragment implements Comparable<Fragment> {

    private final FileInRevision file;
    private final PositionInText from;
    private final PositionInText to;
    private String content;

    Fragment(FileInRevision file, PositionInText from, PositionInText to) {
        assert file != null;
        assert from != null;
        assert to != null;
        this.file = file;
        this.from = from;
        this.to = to;
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
        final byte[] contents = this.file.getContents();
        if (contents == null) {
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
                        if (this.to.getColumn() > 0) {
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
        return this.from + " - " + this.to + " in " + this.file;
    }

    /**
     * Returns true iff this fragments is a direct neighbor of the given other fragment, but
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
        return this.to.compareTo(other.from) >= 0 && this.from.compareTo(other.to) <= 0;
    }

    /**
     * Returns true if this fragment is adjacent to the passed one. Adjacent fragments do not overlap.
     * @param other The other fragment.
     * @return True if fragments are adjacent, else false.
     */
    public boolean isAdjacentTo(final Fragment other) {
        return this.to.nextInLine().equals(other.from) || this.from.equals(other.to.nextInLine());
    }

    /**
     * Adjoins two adjacent fragments. The associated FileInRevision of the resulting fragment is taken from this
     * fragment.
     * @param other The other fragment-
     * @return The adjoint fragment encompassing both original fragments.
     */
    public Fragment adjoin(final Fragment other) {
        assert this.isAdjacentTo(other);
        if (this.to.nextInLine().equals(other.from)) {
            return new Fragment(this.file, this.from, other.to);
        } else {
            return new Fragment(this.file, other.from, this.to);
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
            final FragmentList fragmentList = new FragmentList();
            try {
                if (this.from.lessThan(other.from)) {
                    fragmentList.addFragment(new Fragment(this.file, this.from, other.from.prevInLine()));
                }
                if (other.to.lessThan(this.to)) {
                    fragmentList.addFragment(new Fragment(this.file, other.to.nextInLine(), this.to));
                }
            } catch (final IncompatibleFragmentException e) {
                throw new Error(e);
            }
            return fragmentList;
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
        return !(this.to.nextInLine().lessThan(other.from)
            || other.to.nextInLine().lessThan(this.from));
    }

    /**
     * Creates a new fragment combining the area in the file spanned by this
     * and the given other fragment.
     */
    public Fragment merge(Fragment other) {
        if (other.from.lessThan(this.from)) {
            return other.merge(this);
        }

        assert this.canBeMergedWith(other);
        final PositionInText minFrom = this.from;
        final PositionInText maxTo;
        if (this.to.lessThan(other.to)) {
            maxTo = other.to;
        } else {
            maxTo = this.to;
        }
        return new Fragment(this.file, minFrom, maxTo);
    }

    public boolean isDeletion() {
        return this.to.lessThan(this.from);
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
            && this.to.equals(other.to);
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
     * Creates a new fragment whose start and end positions are shifted by the given line offset.
     * @param offset The line offset to add.
     * @return The resulting fragment.
     */
    public Fragment adjust(final int offset) {
        return new Fragment(this.file, this.from.adjust(offset), this.to.adjust(offset));
    }

    /**
     * Creates a new fragment whose start and end positions are shifted by the given column offset
     * if the respective position is in the given targetLine.
     */
    public Fragment adjustColumnIfInLine(final int offset, int targetLine) {
        return new Fragment(this.file,
                this.from.getLine() == targetLine ? this.from.adjustColumn(offset) : this.from,
                this.to.getLine() == targetLine ? this.to.adjustColumn(offset) : this.to);
    }

    @Override
    public int compareTo(final Fragment o) {
        final int from = this.getFrom().compareTo(o.getFrom());
        return from != 0 ? from : this.getTo().compareTo(o.getTo());
    }

}
