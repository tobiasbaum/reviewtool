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

    public Fragment(FileInRevision file, PositionInText from, PositionInText to) {
        this.file = file;
        this.from = from;
        this.to = to;
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

    @Override
    public String toString() {
        return this.from + " - " + this.to + " in " + this.file;
    }

}
