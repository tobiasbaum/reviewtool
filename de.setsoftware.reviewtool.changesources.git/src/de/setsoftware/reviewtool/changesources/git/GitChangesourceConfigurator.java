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

        final String user = xml.getAttribute("user");
        final String pwd = xml.getAttribute("password");
        final String pattern = xml.getAttribute("pattern");
        final String maxTextDiffThreshold = xml.getAttribute("maxTextDiffFileSizeThreshold");
        final String minLogCacheSize = xml.getAttribute("minLogCacheSize");

        configurable.setChangeSource(new GitChangeSource(
                pattern, 
                user, 
                pwd,
                Long.parseLong(maxTextDiffThreshold),
                minLogCacheSize.isEmpty() ? 1000 : Integer.parseInt(minLogCacheSize)));
    }
}
