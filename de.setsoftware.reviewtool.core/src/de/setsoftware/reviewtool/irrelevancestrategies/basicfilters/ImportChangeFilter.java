package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;
import de.setsoftware.reviewtool.model.changestructure.TextualChangeHunk;

/**
 * Filter that marks changes as irrelevant which only consist of lines starting with "import ".
 */
public class ImportChangeFilter extends IIrrelevanceDetermination {

    public ImportChangeFilter(int number) {
        super(number);
    }

    @Override
    public String getDescription() {
        return "ignore changes in imports";
    }

    @Override
    public boolean isIrrelevant(IChange change) {
        if (change instanceof TextualChangeHunk) {
            final ITextualChange hunk = (ITextualChange) change;
            return this.isOnlyImports(hunk.getFromFragment()) && this.isOnlyImports(hunk.getToFragment())
                && !(this.isOnlyWhitespace(hunk.getFromFragment()) && this.isOnlyWhitespace(hunk.getToFragment()));
        }
        return false;
    }

    private boolean isOnlyImports(IFragment fragment) {
        final BufferedReader r = new BufferedReader(new StringReader(fragment.getContentFullLines()));
        String line;
        try {
            while ((line = r.readLine()) != null) {
                final String trimmed = line.trim();
                if (!(trimmed.isEmpty() || trimmed.startsWith("import "))) {
                    return false;
                }
            }
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
        return true;
    }

    private boolean isOnlyWhitespace(IFragment fragment) {
        return fragment.getContentFullLines().trim().isEmpty();
    }

}
