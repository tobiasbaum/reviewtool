package de.setsoftware.reviewtool.dialogs;

/**
 * Callback that is called when the uses finishes input into an input dialog.
 */
public interface InputDialogCallback {
    /**
     * Perform an action with the given user provided input.
     */
    public abstract void execute(String text);
}