package de.setsoftware.reviewtool.model.changestructure;

/**
 * Visits a {@link FileHistoryNode}.
 */
public interface FileHistoryNodeVisitor {

    /**
     * Handles an {@link ExistingFileHistoryNode}.
     */
    public abstract void handleExistingNode(ExistingFileHistoryNode node);

    /**
     * Handles a {@link NonExistingFileHistoryNode}.
     */
    public abstract void handleNonExistingNode(NonExistingFileHistoryNode node);
}