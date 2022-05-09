package de.setsoftware.reviewtool.preferredtransitions.basicstrategies;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for {@link PathRegexAndReviewerCountStrategy}.
 */
public class PathRegexStrategyConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singletonList("preferredEndTransitionPattern");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        final String maxReviewerCount = xml.getAttribute("maxReviewerCount");
        configurable.configureWith(new PathRegexAndReviewerCountStrategy(
                xml.getAttribute("pattern"),
                xml.getAttribute("transitionName"),
                maxReviewerCount.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(maxReviewerCount)));
    }

}
