package de.setsoftware.reviewtool.ui.dialogs.extensions.surveyatend;

/**
 * An answer possibility for a survey question.
 */
public class Answer {

    private final String id;
    private final String text;

    public Answer(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return this.id;
    }

    public String getText() {
        return this.text;
    }

}
