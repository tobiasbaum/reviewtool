package de.setsoftware.reviewtool.model;

/**
 * Basic information for a ticket.
 */
public class TicketInfo {

    private final String key;
    private final String description;
    private final String state;
    private final String component;
    private final String parentSummary;

    public TicketInfo(String key, String summary, String state, String component,
            String parentSummary) {
        this.key = key;
        this.description = summary;
        this.state = state;
        this.component = component;
        this.parentSummary = parentSummary;
    }

    public String getId() {
        return this.key;
    }

    public String getSummary() {
        return this.description;
    }

    /**
     * Returns the ticket's description, preceded by the parent's summary, if a parent exists.
     */
    public String getSummaryIncludingParent() {
        if (this.parentSummary != null) {
            return this.parentSummary + " - " + this.description;
        } else {
            return this.description;
        }
    }

    public String getState() {
        return this.state;
    }

    public String getComponent() {
        return this.component;
    }

}
