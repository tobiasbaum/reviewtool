package de.setsoftware.reviewtool.model.changestructure;

import java.util.Arrays;

import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChangeVisitor;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Default implementation of {@link IBinaryChange}.
 */
public class BinaryChange extends Change implements IBinaryChange {

    private final IRevisionedFile from;
    private final IRevisionedFile to;

    BinaryChange(
            final IWorkingCopy wc,
            final FileChangeType fileChangeType,
            final IRevisionedFile from,
            final IRevisionedFile to,
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
    public IRevisionedFile getFrom() {
        return this.from;
    }

    @Override
    public IRevisionedFile getTo() {
        return this.to;
    }

    @Override
    public IBinaryChange addClassification(IClassification cl) {
        if (Arrays.asList(this.getClassification()).contains(cl)) {
            return this;
        }
        return new BinaryChange(
                this.getWorkingCopy(), this.getType(), this.from, this.to, this.concatClassification(cl));
    }

    @Override
    public int hashCode() {
        return this.from.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BinaryChange)) {
            return false;
        }
        final BinaryChange t = (BinaryChange) o;
        return this.from.equals(t.from)
            && this.to.equals(t.to)
            && super.equals(o);
    }

    @Override
    public String toString() {
        return this.from + "\n" + this.to;
    }
}
