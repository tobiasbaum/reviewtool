package de.setsoftware.reviewtool.model;

import java.util.Date;
import java.util.Set;

/**
 * Basic information for a ticket.
 */
public class TicketInfo {

    private static final long MS_PER_DAY = 24L * 60 * 60 * 1000;

    private final String key;
    private final String description;
    private final String state;
    private final String previousState;
    private final String component;
    private final String parentSummary;
    private final Set<String> reviewers;
    private final Date waitingSince;

    public TicketInfo(String key, String summary, String state, String previousState, String component,
            String parentSummary, Set<String> reviewers, Date waitingSince) {
        this.key = key;
        this.description = summary;
        this.state = state;
        this.previousState = previousState;
        this.component = component;
        this.parentSummary = parentSummary;
        this.reviewers = reviewers;
        this.waitingSince = waitingSince;
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

    public Date getWaitingSince() {
        return this.waitingSince;
    }

    public int getWaitingForDays(Date date) {
        return (int) Math.round(((double) (date.getTime() - this.waitingSince.getTime())) / MS_PER_DAY);
    }

}
