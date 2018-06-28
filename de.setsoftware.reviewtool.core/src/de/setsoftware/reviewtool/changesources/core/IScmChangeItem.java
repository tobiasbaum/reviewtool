package de.setsoftware.reviewtool.changesources.core;

import java.io.Serializable;

/**
 * Describes an item in a {@link IScmCommit}.
 */
public interface IScmChangeItem extends Serializable {

    /**
     * Returns the path of this item relative to the repository root.
     */
    public abstract String getPath();

    /**
     * Returns {@code true} iff this item denotes a file.
     */
    public abstract boolean isFile();

    /**
     * Returns {@code true} iff this item denotes a directory.
     */
    public abstract boolean isDirectory();

    /**
     * Returns {@code true} iff this item has been added.
     * Note that {@link #isDeleted()} can return {@code true} at the same time to indicate a replacement.
     */
    public abstract boolean isAdded();

    /**
     * Returns {@code true} iff this item has been changed.
     */
    public abstract boolean isChanged();

    /**
     * Returns {@code true} iff this item has been deleted.
     * Note that {@link #isAdded()} can return {@code true} at the same time to indicate a replacement.
     */
    public abstract boolean isDeleted();

    /**
     * Returns the string representation of a change item.
     */
    @Override
    public abstract String toString();
}
