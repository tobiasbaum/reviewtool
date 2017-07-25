package de.setsoftware.reviewtool.irrelevancestrategies.pathfilters;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Aktiviert die Basis-Filter aus diesem Paket.
 */
public class PathIrrelevanceFilterConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singleton("pathIrrelevanceFilter");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        configurable.addIrrelevanceStrategy(
                new PathFilter(xml.getAttribute("pattern"), xml.getAttribute("description")));
    }

}
