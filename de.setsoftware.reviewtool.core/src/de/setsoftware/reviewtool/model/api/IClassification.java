package de.setsoftware.reviewtool.model.api;

/**
 * A type for classifying hunks and stops.
 * The purpose of classifications is two-fold:
 * They add information for the reviewer, and they can be used in a second step to find stops that don't need to be reviewed or that
 * need to be reviewed intensively.
 */
public interface IClassification {

    /**
     * Returns the name that identifies this classification type uniquely.
     */
    public abstract String getName();

    /**
     * Determines the behavior when merging stops of which only one has this classification.
     * If this method returns true, the classification is only kept when both stops have it.
     * Otherwise, only one occurrence suffices.
     */
    public abstract boolean mergeAsAnd();

}
