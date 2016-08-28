package de.setsoftware.reviewtool.model;

/**
 * A comment on a review remark.
 * Each comment knows the user who authored it and also has a text.
 */
public class ReviewRemarkComment {

    private final String user;
    private final String text;

    ReviewRemarkComment(String user, String text) {
        this.user = user;
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public String getUser() {
        return this.user;
    }

    public String getText() {
        return this.text;
    }

}
