package de.setsoftware.reviewtool.irrelevancestrategies.pathfilters;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for "fileCountInCommitFilter".
 */
public class FileCountInCommitFilterConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singleton("fileCountInCommitFilter");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        configurable.addClassificationStrategy(new FileCountInCommitFilter(
                Integer.parseInt(xml.getAttribute("number")),
                xml.getAttribute("description"),
                Integer.parseInt(xml.getAttribute("threshold"))));
    }

}
