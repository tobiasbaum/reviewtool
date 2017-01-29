package de.setsoftware.reviewtool.reminder;

import java.util.Collection;
import java.util.Collections;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for a {@link Reminder}.
 */
public class ReminderConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singleton("reminder");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        final int minCount = Integer.parseInt(xml.getAttribute("minCount"));
        configurable.addPostInitTask(new Reminder(minCount));
    }

}
