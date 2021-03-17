package de.setsoftware.reviewtool.ticketconnectors.jira;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;
import de.setsoftware.reviewtool.model.TicketLinkSettings;

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
        final String ticketLinkPattern = xml.getAttribute("ticketLinkPattern");
        final String ticketLinkText = xml.getAttribute("ticketLinkText");
        final TicketLinkSettings linkSettings;
        if (!ticketLinkPattern.isEmpty()) {
            linkSettings = new TicketLinkSettings(
                    ticketLinkPattern,
                    ticketLinkText.isEmpty() ? "Open ticket" : ticketLinkText);
        } else {
            linkSettings = null;
        }

        final JiraPersistence p = new JiraPersistence(
                xml.getAttribute("url"),
                xml.getAttribute("reviewRemarkField"),
                xml.getAttribute("reviewState"),
                xml.getAttribute("implementationState"),
                xml.getAttribute("readyForReviewState"),
                xml.getAttribute("rejectedState"),
                xml.getAttribute("doneState"),
                xml.getAttribute("user"),
                xml.getAttribute("password"),
                linkSettings,
                this.toFile(xml.getAttribute("cookieFile")));
        final NodeList filters = xml.getElementsByTagName("filter");
        for (int i = 0; i < filters.getLength(); i++) {
            final Element filter = (Element) filters.item(i);
            p.addFilter(
                    filter.getAttribute("name"),
                    filter.getAttribute("jql"),
                    Boolean.parseBoolean(filter.getAttribute("forReview")));
        }
        configurable.configureWith(p);
    }

    private File toFile(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return new File(attribute);
    }

}
