package de.setsoftware.reviewtool.model.changestructure;

/**
 * Represents a review element.
 */
public interface IReviewElement {

    /**
     * Returns true if this element is visible. Invisible elementswould have been filtered out if they did not take
     * part in the relevant change history. For example, if a file A was changed in revisions 1-3, and the revisions 1
     * and 3 belong to ticket A and revision 2 belongs to ticket B, and someone wants to analyse the changes of ticket
     * A, the changes in revision 2 need to be included because the changes in revision 3 depend upon them, but these
     * changes in revision 2 are marked as invisible as they are not part of ticket A.
     */
    boolean isVisible();

}
