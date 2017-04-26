package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Interface for the results from getting the relevant changes for a ticket from the ticket system.
 */
public interface IChangeData {

    /**
     * Returns all commits that were matching for the given ticket.
     */
    public abstract List<Commit> getMatchedCommits();

    /**
     * Creates a fragment tracer for the change source.
     * The fragment tracer can use data cached when determining the matched commits, but does not have to.
     */
    public abstract IFragmentTracer createTracer();

    /**
     * Returns a {@link IFileHistoryGraph} for the change data.
     */
    public abstract IFileHistoryGraph getHistoryGraph();

}
