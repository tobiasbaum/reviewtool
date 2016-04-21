package de.setsoftware.reviewtool.connectors.file;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.w3c.dom.Element;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * Configurator for file persistence.
 */
public class FileTicketConnectorConfigurator implements IConfigurator {

    @Override
    public Set<String> getRelevantElementNames() {
        return Collections.singleton("fileTicketStore");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        configurable.setPersistence(new FilePersistence(
                new File(xml.getAttribute("directory")),
                xml.getAttribute("defaultReviewer")));
    }

}
