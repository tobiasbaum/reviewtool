package de.setsoftware.reviewtool.model;

/**
 * A transition to end the review of a ticket.
 */
public class EndTransition {

    /**
     * The type of review result this transitions stands for.
     */
    public static enum Type {
        /**
         * The review was OK.
         */
        OK,
        /**
         * The ticket has remarks the need fixing.
         */
        REJECTION,
        /**
         * The ticket's state shall not be changed.
         */
        PAUSE,
        /**
         * The semantic of the transition is unknown.
         */
        UNKNOWN
    }

    private final String nameForUser;
    private final String internalName;
    private final Type type;

    /**
     * Constructor.
     * @param nameForUser Name of the transition that is shown to the user.
     * @param internalName Internal key for the transition.
     * @param type Type defining the semantic of the transition.
     */
    public EndTransition(String nameForUser, String internalName, Type type) {
        this.nameForUser = nameForUser;
        this.internalName = internalName;
        this.type = type;
    }

    public String getInternalName() {
        return this.internalName;
    }

    public Type getType() {
        return this.type;
    }

    public String getNameForUser() {
        return this.nameForUser;
    }

}
