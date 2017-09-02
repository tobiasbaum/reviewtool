package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IChangeVisitor;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.ITextualChange;

/**
 * Default implementation of {@link ITextualChange}.
 */
public class TextualChangeHunk extends Change implements ITextualChange {

    private final IFragment from;
    private final IFragment to;

    TextualChangeHunk(
            final IFragment from, final IFragment to, final boolean irrelevantForReview, final boolean isVisible) {
        super(irrelevantForReview, isVisible);
        this.from = from;
        this.to = to;
    }

    @Override
    public void accept(IChangeVisitor visitor) {
        visitor.handle(this);
    }

    @Override
    public IFragment getFromFragment() {
        return this.from;
    }

    @Override
    public IFragment getToFragment() {
        return this.to;
    }

    @Override
    public ITextualChange makeIrrelevant() {
        if (this.isIrrelevantForReview()) {
            return this;
        }
        return new TextualChangeHunk(this.from, this.to, true, this.isVisible());
    }

    @Override
    public IRevisionedFile getFrom() {
        return this.getFromFragment().getFile();
    }

    @Override
    public IRevisionedFile getTo() {
        return this.getToFragment().getFile();
    }

}
