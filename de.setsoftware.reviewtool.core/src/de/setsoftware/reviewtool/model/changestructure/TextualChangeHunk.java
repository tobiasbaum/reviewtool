package de.setsoftware.reviewtool.model.changestructure;

import java.util.Arrays;

import de.setsoftware.reviewtool.model.api.IChangeVisitor;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Default implementation of {@link ITextualChange}.
 */
public class TextualChangeHunk extends Change implements ITextualChange {

    private final IFragment from;
    private final IFragment to;

    TextualChangeHunk(
            final IWorkingCopy wc,
            final IFragment from,
            final IFragment to,
            final IClassification[] classification) {
        super(wc, classification);
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
    public ITextualChange addClassification(IClassification cl) {
        if (Arrays.asList(this.getClassification()).contains(cl)) {
            return this;
        }
        return new TextualChangeHunk(this.getWorkingCopy(), this.from, this.to, this.concatClassification(cl));
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
