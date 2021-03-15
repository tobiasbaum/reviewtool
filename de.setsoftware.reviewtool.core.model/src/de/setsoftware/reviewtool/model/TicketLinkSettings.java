package de.setsoftware.reviewtool.model;

/**
 * Contains date needed to create a link to open ticket details, based on the ticket's ID.
 */
public class TicketLinkSettings {

    private final String buttonText;
    private final String urlPattern;

    public TicketLinkSettings(String urlPattern, String buttonText) {
        this.urlPattern = urlPattern;
        this.buttonText = buttonText;
    }

    public String getText() {
        return this.buttonText;
    }

    public String createLinkFor(String id) {
        return String.format(this.urlPattern, id);
    }

}
