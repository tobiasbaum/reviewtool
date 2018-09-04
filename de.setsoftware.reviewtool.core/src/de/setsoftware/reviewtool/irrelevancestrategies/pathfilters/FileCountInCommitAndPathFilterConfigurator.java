package de.setsoftware.reviewtool.irrelevancestrategies.pathfilters;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for "fileCountInCommitAndPathFilter".
 */
public class FileCountInCommitAndPathFilterConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singleton("fileCountInCommitAndPathFilter");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        final FileCountInCommitFilter f1 = new FileCountInCommitFilter(
                Integer.parseInt(xml.getAttribute("number")),
                xml.getAttribute("description"),
                Integer.parseInt(xml.getAttribute("threshold")));
        final PathFilter f2 = new PathFilter(
                Integer.parseInt(xml.getAttribute("number")),
                xml.getAttribute("pattern"),
                xml.getAttribute("description"));
        configurable.addClassificationStrategy(new AndFilter(f1, f2));
    }

}
