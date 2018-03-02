package de.setsoftware.reviewtool.model.api;

/**
 * A singular change of a part of a text file.
 */
public interface ITextualChange extends IChange {

    /**
     * Returns the source fragment.
     */
    public abstract IFragment getFromFragment();

    /**
     * Returns the target fragment.
     */
    public abstract IFragment getToFragment();

}
