package de.setsoftware.reviewtool.model.changestructure;

import java.util.Arrays;

import de.setsoftware.reviewtool.model.api.FileChangeType;
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
            final FileChangeType fileChangeType,
            final IFragment from,
            final IFragment to,
            final IClassification[] classification) {
        super(wc, fileChangeType, classification);
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
        return new TextualChangeHunk(
                this.getWorkingCopy(), this.getType(), this.from, this.to, this.concatClassification(cl));
    }

    @Override
    public IRevisionedFile getFrom() {
        return this.getFromFragment().getFile();
    }

    @Override
    public IRevisionedFile getTo() {
        return this.getToFragment().getFile();
    }

    @Override
    public String toString() {
        return this.from + "\n" + this.to;
    }

    @Override
    public int hashCode() {
        return this.from.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TextualChangeHunk)) {
            return false;
        }
        final TextualChangeHunk t = (TextualChangeHunk) o;
        return this.from.equals(t.from)
            && this.to.equals(t.to)
            && super.equals(o);
    }
}
