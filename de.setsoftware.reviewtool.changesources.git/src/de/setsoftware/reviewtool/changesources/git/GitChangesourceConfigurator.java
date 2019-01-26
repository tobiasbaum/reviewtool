package de.setsoftware.reviewtool.changesources.git;

import java.util.Collections;
import java.util.Set;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for this package.
 */
public class GitChangesourceConfigurator implements IConfigurator {

    @Override
    public Set<String> getRelevantElementNames() {
        return Collections.singleton("gitChangeSource");
    }

    @Override
    public void configure(final Element xml, final IReviewConfigurable configurable) {

        final String pattern = xml.getAttribute("pattern");
        final String maxTextDiffThreshold = xml.getAttribute("maxTextDiffFileSizeThreshold");

        configurable.setChangeSource(new GitChangeSource(
                pattern,
                Long.parseLong(maxTextDiffThreshold)));
    }
}
