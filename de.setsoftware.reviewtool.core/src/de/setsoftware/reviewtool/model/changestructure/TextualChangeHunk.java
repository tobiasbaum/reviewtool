package de.setsoftware.reviewtool.model.changestructure;

/**
 * A singular change of a part of a text file.
 */
public class TextualChangeHunk extends Change {

    private final Fragment from;
    private final Fragment to;

    TextualChangeHunk(Fragment from, Fragment to, boolean irrelevantForReview) {
        super(irrelevantForReview);
        this.from = from;
        this.to = to;
    }

    @Override
    public void accept(ChangeVisitor visitor) {
        visitor.handle(this);
    }

    public Fragment getFromFragment() {
        return this.from;
    }

    public Fragment getToFragment() {
        return this.to;
    }

    @Override
    protected TextualChangeHunk makeIrrelevant() {
        if (this.isIrrelevantForReview()) {
            return this;
        }
        return new TextualChangeHunk(this.from, this.to, true);
    }

    public FileInRevision getFrom() {
        return this.getFromFragment().getFile();
    }

    @Override
    public FileInRevision getTo() {
        return this.getToFragment().getFile();
    }

}
