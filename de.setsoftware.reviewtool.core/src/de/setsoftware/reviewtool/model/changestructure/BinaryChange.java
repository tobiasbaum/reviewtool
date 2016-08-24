package de.setsoftware.reviewtool.model.changestructure;

/**
 * A change in a binary file for which no diff is available.
 */
public class BinaryChange extends Change {

    private final FileInRevision from;
    private final FileInRevision to;

    BinaryChange(FileInRevision from, FileInRevision to, boolean irrelevantForReview, final boolean isVisible) {
        super(irrelevantForReview, isVisible);
        this.from = from;
        this.to = to;
    }

    @Override
    public void accept(ChangeVisitor visitor) {
        visitor.handle(this);
    }

    @Override
    public FileInRevision getFrom() {
        return this.from;
    }

    @Override
    public FileInRevision getTo() {
        return this.to;
    }

    @Override
    protected BinaryChange makeIrrelevant() {
        if (this.isIrrelevantForReview()) {
            return this;
        }
        return new BinaryChange(this.from, this.to, true, this.isVisible());
    }
}
