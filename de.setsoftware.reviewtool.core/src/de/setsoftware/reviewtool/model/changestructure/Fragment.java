package de.setsoftware.reviewtool.model.changestructure;

/**
 * A fragment is the smallest unit of a change. A fragment is generally checked as a whole by a reviewer,
 * and separately from other fragments. Examples for fragments are methods that are new or changed considerably,
 * but also single lines or even parts of expressions, if there was only a small change in that area of the code.
 * A fragment denotes a continuous portion of a file in a specific revision. If the change for this fragment was
 * a pure deletion, so that there is no code to point to left in that revision of the file, this is denoted
 * specially.
 */
public class Fragment {

    private final FileInRevision file;
    private final PositionInText from;
    private final PositionInText to;
    private final String content;

    public Fragment(FileInRevision file, PositionInText from, PositionInText to, String content) {
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

}
