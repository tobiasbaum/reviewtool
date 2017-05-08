package de.setsoftware.reviewtool.model.api;

/**
 * A singular change of a part of a text file.
 */
public interface ITextualChange extends IChange {

    /**
     * @return The source fragment.
     */
    public abstract IFragment getFromFragment();

    /**
     * @return The target fragment.
     */
    public abstract IFragment getToFragment();

}
