package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for "fileDeletionFilter".
 */
public class FileDeletionFilterConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singleton("fileDeletionFilter");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        configurable.configureWith(
                new FileDeletionFilter(Integer.parseInt(xml.getAttribute("number"))));
    }

}
