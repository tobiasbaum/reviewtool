package de.setsoftware.reviewtool.ui.dialogs.extensions.surveyatend;

import java.util.ArrayList;
import java.util.List;

/**
 * A multiple choice question for a survey.
 */
public class Question {

    private final String id;
    private final String text;
    private final List<Answer> choices = new ArrayList<>();

    public Question(String id, String text) {
        this.id = id;
        this.text = text;
    }

    void addChoice(Answer answer) {
        this.choices.add(answer);
    }

    public String getId() {
        return this.id;
    }

    public String getText() {
        return this.text;
    }

    /**
     * Determines the ID of the answer possibility with the given text.
     * If none can be found, null is returned.
     */
    public String getIdForChoiceText(String choiceText) {
        for (final Answer c : this.choices) {
            if (c.getText().equals(choiceText)) {
                return c.getId();
            }
        }
        return null;
    }

    public List<Answer> getChoices() {
        return this.choices;
    }
}
