package de.setsoftware.reviewtool.model.changestructure;

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
    private final String content;

    Fragment(FileInRevision file, PositionInText from, PositionInText to, String content) {
        this.file = file;
        this.from = from;
        this.to = to;
        this.content = content;
    }

    public FileInRevision getFile() {
        return this.file;
    }

    public PositionInText getFrom() {
        return this.from;
    }

    public PositionInText getTo() {
        return this.to;
    }

    public String getContent() {
        return this.content;
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
        return this.to.nextInLine().equals(other.from)
            || other.to.nextInLine().equals(this.from);
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
            return new Fragment(this.file, this.from, other.to, this.content + other.content);
        } else {
            return new Fragment(this.file, other.from, this.to, other.content + this.content);
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
                    fragmentList.addFragment(new Fragment(this.file, this.from, other.from.prevInLine(),
                            this.subContentTo(other.from.prevInLine())));
                }
                if (other.to.lessThan(this.to)) {
                    fragmentList.addFragment(new Fragment(this.file, other.to.nextInLine(), this.to,
                            this.subContentFrom(other.to.nextInLine())));
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
        final String combinedContent;
        if (this.to.lessThan(other.to)) {
            maxTo = other.to;
            combinedContent = this.content + other.subContentFrom(this.to);
        } else {
            maxTo = this.to;
            combinedContent = this.content;
        }
        return new Fragment(this.file, minFrom, maxTo, combinedContent);
    }

    private String subContentFrom(PositionInText pos) {
        String remainingContent = this.content;
        PositionInText remainingFrom = this.from;

        while (remainingFrom.getLine() < pos.getLine()) {
            final int linebreak = remainingContent.indexOf('\n');
            remainingContent = remainingContent.substring(linebreak + 1);
            remainingFrom = new PositionInText(remainingFrom.getLine() + 1, 1);
        }
        final int diff = pos.getColumn() - this.from.getColumn();
        if (diff < 0) {
            return remainingContent;
        }
        return remainingContent.substring(diff);
    }

    /**
     * Returns the fragment contents from the beginning of the fragment up to the passed {@link PositionInText}
     * (inclusive).
     * @param pos The end of the desired range.
     * @return The contents.
     */
    private String subContentTo(final PositionInText pos) {
        int lastOffset = 0;
        int line = this.from.getLine();

        while (line < pos.getLine()) {
            lastOffset = this.content.indexOf('\n', lastOffset) + 1;
            ++line;
        }
        return this.content.substring(0, lastOffset + pos.getColumn());
    }

    private boolean isDeletion() {
        return this.to.lessThan(this.from);
    }

    @Override
    public int hashCode() {
        return this.from.hashCode() + 31 * this.content.hashCode();
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
            && this.content.equals(other.content);
    }

    /**
     * Returns the number of lines in this fragment. For deletion fragments, zero is returned.
     */
    public int getNumberOfLines() {
        final int rawLineDiff = this.to.getLine() - this.from.getLine();
        if (this.to.getColumn() > this.from.getColumn()) {
            return rawLineDiff + 1;
        } else {
            return rawLineDiff;
        }
    }

    /**
     * Creates a new fragment whose start and end positions are shifted by the given line offset.
     * @param offset The line offset to add.
     * @return The resulting fragment.
     */
    public Fragment adjust(final int offset) {
        return new Fragment(this.file, this.from.adjust(offset), this.to.adjust(offset), this.content);
    }

    @Override
    public int compareTo(final Fragment o) {
        final int from = this.getFrom().compareTo(o.getFrom());
        return from != 0 ? from : this.getTo().compareTo(o.getTo());
    }

}
