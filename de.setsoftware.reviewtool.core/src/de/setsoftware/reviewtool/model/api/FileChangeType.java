package de.setsoftware.reviewtool.model.api;

/**
 * The type of file change for a change.
 */
public enum FileChangeType {
    /**
     * File was added.
     */
    ADDED,
    /**
     * File was deleted.
     */
    DELETED,
    /**
     * Some other change.
     */
    OTHER
}
