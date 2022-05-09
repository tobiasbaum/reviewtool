package de.setsoftware.reviewtool.model;

/**
 * Interface that provides access to the different types of user interaction.
 */
public interface IUserInteraction {

    public abstract ITicketChooser getTicketChooser();

    public abstract ISyntaxFixer getSyntaxFixer();

}
