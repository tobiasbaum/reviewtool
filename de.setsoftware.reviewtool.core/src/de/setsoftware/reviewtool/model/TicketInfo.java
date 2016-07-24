package de.setsoftware.reviewtool.model;

import java.util.Set;

/**
 * Basic information for a ticket.
 */
public class TicketInfo {

    private final String key;
    private final String description;
    private final String state;
    private final String previousState;
    private final String component;
    private final String parentSummary;
    private final Set<String> reviewers;

    public TicketInfo(String key, String summary, String state, String previousState, String component,
            String parentSummary, Set<String> reviewers) {
        this.key = key;
        this.description = summary;
        this.state = state;
        this.previousState = previousState;
        this.component = component;
        this.parentSummary = parentSummary;
        this.reviewers = reviewers;
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

    public String getPreviousState() {
        return this.previousState;
    }

    public String getComponent() {
        return this.component;
    }

    public Set<String> getReviewers() {
        return this.reviewers;
    }

}
