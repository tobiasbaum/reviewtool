package de.setsoftware.reviewtool.connectors.jira;

import java.util.Collections;
import java.util.Set;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for the JIRA connector.
 */
public class JiraConnectorConfigurator implements IConfigurator {

    @Override
    public Set<String> getRelevantElementNames() {
        return Collections.singleton("jiraTicketStore");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        configurable.setPersistence(new JiraPersistence(
                xml.getAttribute("url"),
                xml.getAttribute("reviewRemarkField"),
                xml.getAttribute("reviewState"),
                xml.getAttribute("implementationState"),
                xml.getAttribute("readyForReviewState"),
                xml.getAttribute("rejectedState"),
                xml.getAttribute("doneState"),
                xml.getAttribute("user"),
                xml.getAttribute("password")));
    }

}
