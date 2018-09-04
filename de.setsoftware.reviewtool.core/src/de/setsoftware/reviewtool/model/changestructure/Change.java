package de.setsoftware.reviewtool.model.changestructure;

import java.util.Arrays;

import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * A singular change in a commit.
 */
public abstract class Change implements IChange {

    private final IWorkingCopy wc;
    private final FileChangeType fileChangeType;
    private final IClassification[] classification;

    /**
     * Constructs a change.
     */
    public Change(
            final IWorkingCopy wc, FileChangeType fileChangeType, final IClassification[] classification) {
        this.wc = wc;
        this.fileChangeType = fileChangeType;
        this.classification = classification;
    }

    protected IClassification[] concatClassification(IClassification cl) {
        final IClassification[] ret = Arrays.copyOf(this.classification, this.classification.length + 1);
        ret[this.classification.length] = cl;
        return ret;
    }

    @Override
    public final IClassification[] getClassification() {
        return this.classification;
    }

    @Override
    public final IWorkingCopy getWorkingCopy() {
        return this.wc;
    }

    @Override
    public final FileChangeType getType() {
        return this.fileChangeType;
    }

}
