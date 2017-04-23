package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import de.setsoftware.reviewtool.model.changestructure.Change;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;
import de.setsoftware.reviewtool.model.changestructure.TextualChangeHunk;

/**
 * Filter that marks changes as irrelevant which only contain changes in whitespace.
 */
public class WhitespaceChangeFilter implements IIrrelevanceDetermination {

    @Override
    public String getDescription() {
        return "ignore whitespace only changes";
    }

    @Override
    public boolean isIrrelevant(Change change) {
        if (change instanceof TextualChangeHunk) {
            final TextualChangeHunk hunk = (TextualChangeHunk) change;
            return this.normalizeWhitespace(hunk.getFromFragment()).equals(
                    this.normalizeWhitespace(hunk.getToFragment()));
        }
        return false;
    }

    private String normalizeWhitespace(Fragment fragment) {
        return fragment.getContentFullLines().replaceAll("[ \r\n\t]+", " ").trim();
    }

}
