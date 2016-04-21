package de.setsoftware.reviewtool.config;

import java.util.Set;

import org.w3c.dom.Element;

/**
 * Interface for classes that interpret parts of the tool configuration.
 */
public interface IConfigurator {

    /**
     * Returns the element names this configurator wants to receive.
     */
    public abstract Set<String> getRelevantElementNames();

    /**
     * Configures something based on the given XML configuration snippet.
     * Will only be called for elements that have one of the relevant names.
     */
    public abstract void configure(Element xml, IReviewConfigurable configurable);

}
