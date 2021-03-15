package de.setsoftware.reviewtool.ui.views;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configures the stop viewer to use.
 */
public class StopViewConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singleton("stopViewer");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        final String type = xml.getAttribute("type");
        if (type.equals("combined")) {
            configurable.configureWith(new CombinedDiffStopViewer());
        }
    }

}
