package de.setsoftware.reviewtool.irrelevancestrategies.basicfilters;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for "binaryFileFilter".
 */
public class BinaryFileFilterConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singleton("binaryFileFilter");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        configurable.addClassificationStrategy(
                new BinaryFileFilter(Integer.parseInt(xml.getAttribute("number"))));
    }

}
