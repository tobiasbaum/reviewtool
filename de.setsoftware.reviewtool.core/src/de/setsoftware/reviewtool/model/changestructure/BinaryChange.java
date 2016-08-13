package de.setsoftware.reviewtool.model.changestructure;

/**
 * A change in a binary file for which no diff is available.
 */
public class BinaryChange extends Change {

    private final FileInRevision from;
    private final FileInRevision to;

    BinaryChange(FileInRevision from, FileInRevision to, boolean irrelevantForReview) {
        super(irrelevantForReview);
        this.from = from;
        this.to = to;
    }

    @Override
    public void accept(ChangeVisitor visitor) {
        visitor.handle(this);
    }

    public FileInRevision getFrom() {
        return this.from;
    }

    public FileInRevision getTo() {
        return this.to;
    }
}
