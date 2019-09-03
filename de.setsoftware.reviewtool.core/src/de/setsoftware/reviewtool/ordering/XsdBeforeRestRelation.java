package de.setsoftware.reviewtool.ordering;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Ensures that .xsd files (XML schema) come before other files.
 * This is a rough approximation of the real relation that XML schema often defines interface that
 * are used in the rest of the source code.
 */
public class XsdBeforeRestRelation implements RelationMatcher {

    private final HierarchyExplicitness explicitness;

    public XsdBeforeRestRelation(HierarchyExplicitness explicitness) {
        this.explicitness = explicitness;
    }

    @Override
    public Collection<? extends OrderingInfo> determineMatches(
            List<ChangePart> changeParts, TourCalculatorControl control) {

        final Set<ChangePart> xsd = new LinkedHashSet<>();
        final Set<ChangePart> other = new LinkedHashSet<>();

        for (final ChangePart c : changeParts) {
            if (c.isFullyIrrelevantForReview()) {
                continue;
            }
            //all stops of a change part should be in the same file, so the first is sufficient
            final IRevisionedFile file = c.getStops().get(0).getMostRecentFile();
            if (file.getPath().endsWith(".xsd")) {
                xsd.add(c);
            } else {
                other.add(c);
            }
        }

        if (xsd.isEmpty()) {
            return Collections.emptySet();
        } else {
            return Collections.singleton(OrderingInfoImpl.bigStar(this.explicitness, "xsd before rest", xsd, other));
        }
    }

}
