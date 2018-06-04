package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChangeVisitor;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Default implementation of {@link IBinaryChange}.
 */
public class BinaryChange extends Change implements IBinaryChange {

    private final IRevisionedFile from;
    private final IRevisionedFile to;

    BinaryChange(
            final IRevisionedFile from,
            final IRevisionedFile to,
            final boolean irrelevantForReview) {
        super(irrelevantForReview);
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
    public IBinaryChange makeIrrelevant() {
        if (this.isIrrelevantForReview()) {
            return this;
        }
        return new BinaryChange(this.from, this.to, true);
    }

    @Override
    public IRepository getRepository() {
        return this.from.getRepository();
    }
}
