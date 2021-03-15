package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Aktiviert die Basis-Filter aus diesem Paket.
 */
public class BasicIrrelevanceFilterConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singleton("basicIrrelevanceFilters");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        configurable.configureWith(new ImportChangeFilter(1));
        configurable.configureWith(new PackageDeclarationFilter(2));
        configurable.configureWith(new WhitespaceChangeFilter(3));
    }

}
