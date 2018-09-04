package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;

/**
 * Filters out deleted files.
 */
public class FileDeletionFilter extends IIrrelevanceDetermination {

    public FileDeletionFilter(int number) {
        super(number);
    }

    @Override
    public String getDescription() {
        return "deleted file";
    }

    @Override
    public boolean isIrrelevant(ICommit commit, IChange change) {
        return change.getType() == FileChangeType.DELETED;
    }

}
