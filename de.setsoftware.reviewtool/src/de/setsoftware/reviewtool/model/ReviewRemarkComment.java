package de.setsoftware.reviewtool.model;

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
