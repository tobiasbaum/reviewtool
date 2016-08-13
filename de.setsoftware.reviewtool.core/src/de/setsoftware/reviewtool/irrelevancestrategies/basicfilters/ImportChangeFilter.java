package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import de.setsoftware.reviewtool.model.changestructure.Change;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;
import de.setsoftware.reviewtool.model.changestructure.TextualChangeHunk;

/**
 * Filter that marks changes as irrelevant which only consist of lines starting with "import ".
 */
public class ImportChangeFilter implements IIrrelevanceDetermination {

    @Override
    public String getDescription() {
        return "ignore changes in imports";
    }

    @Override
    public boolean isIrrelevant(Change change) {
        if (change instanceof TextualChangeHunk) {
            final TextualChangeHunk hunk = (TextualChangeHunk) change;
            return this.isOnlyImports(hunk.getFrom()) && this.isOnlyImports(hunk.getTo())
                && !(this.isOnlyWhitespace(hunk.getFrom()) && this.isOnlyWhitespace(hunk.getTo()));
        }
        return false;
    }

    private boolean isOnlyImports(Fragment fragment) {
        final BufferedReader r = new BufferedReader(new StringReader(fragment.getContent()));
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

    private boolean isOnlyWhitespace(Fragment fragment) {
        return fragment.getContent().trim().isEmpty();
    }

}
