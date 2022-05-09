package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;
import de.setsoftware.reviewtool.model.changestructure.TextualChangeHunk;

/**
 * Filter that marks changes as irrelevant which only contain changes in whitespace.
 */
public class WhitespaceChangeFilter extends IIrrelevanceDetermination {

    public WhitespaceChangeFilter(int number) {
        super(number);
    }

    @Override
    public String getDescription() {
        return "ignore whitespace only changes";
    }

    @Override
    public boolean isIrrelevant(ICommit commit, IChange change) {
        if (change instanceof TextualChangeHunk) {
            final ITextualChange hunk = (ITextualChange) change;
            return this.normalizeWhitespace(hunk.getFromFragment()).equals(
                    this.normalizeWhitespace(hunk.getToFragment()));
        }
        return false;
    }

    private String normalizeWhitespace(IFragment fragment) {
        return fragment.getContentFullLines().replaceAll("[ \r\n\t]+", "");
    }

}
