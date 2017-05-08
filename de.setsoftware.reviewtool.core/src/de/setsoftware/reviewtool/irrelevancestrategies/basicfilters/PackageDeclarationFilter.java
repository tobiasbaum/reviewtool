package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;
import de.setsoftware.reviewtool.model.changestructure.TextualChangeHunk;

/**
 * Filter that marks changes as irrelevant where only the declared package has changed.
 */
public class PackageDeclarationFilter implements IIrrelevanceDetermination {

    @Override
    public String getDescription() {
        return "ignore changes in package declarations";
    }

    @Override
    public boolean isIrrelevant(IChange change) {
        if (change instanceof TextualChangeHunk) {
            final ITextualChange hunk = (ITextualChange) change;
            return this.isPackageDeclaration(hunk.getFromFragment()) && this.isPackageDeclaration(hunk.getToFragment());
        }
        return false;
    }

    private boolean isPackageDeclaration(IFragment fragment) {
        return fragment.getContentFullLines().trim().matches("package [^;\n]+;");
    }

}
