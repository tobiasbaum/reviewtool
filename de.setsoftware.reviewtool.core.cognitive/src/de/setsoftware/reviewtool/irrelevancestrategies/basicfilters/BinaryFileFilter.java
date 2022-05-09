package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;

/**
 * Filters out binary files.
 */
public class BinaryFileFilter extends IIrrelevanceDetermination {

    public BinaryFileFilter(int number) {
        super(number);
    }

    @Override
    public String getDescription() {
        return "binary file";
    }

    @Override
    public boolean isIrrelevant(ICommit commit, IChange change) {
        return change instanceof IBinaryChange;
    }

}
