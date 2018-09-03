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

}
