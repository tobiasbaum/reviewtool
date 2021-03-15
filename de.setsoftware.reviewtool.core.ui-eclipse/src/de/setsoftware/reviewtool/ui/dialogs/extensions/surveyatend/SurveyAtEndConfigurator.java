package de.setsoftware.reviewtool.ui.dialogs.extensions.surveyatend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;

/**
 * The configurator for the {@link SurveyAtEndExtension}.
 */
public class SurveyAtEndConfigurator implements IConfigurator {

    @Override
    public Collection<String> getRelevantElementNames() {
        return Collections.singletonList("surveyAtReviewEnd");
    }

    @Override
    public void configure(Element xml, IReviewConfigurable configurable) {
        final NodeList questions = xml.getElementsByTagName("question");
        final List<Question> mappedQuestions = new ArrayList<>();
        for (int i = 0; i < questions.getLength(); i++) {
            final Element question = (Element) questions.item(i);
            final Question q = new Question(question.getAttribute("id"), question.getAttribute("text"));
            mappedQuestions.add(q);
            final NodeList choices = question.getElementsByTagName("choice");
            for (int j = 0; j < choices.getLength(); j++) {
                final Element choice = (Element) choices.item(j);
                q.addChoice(new Answer(choice.getAttribute("id"), choice.getAttribute("text")));
            }
        }
        configurable.configureWith(new SurveyAtEndExtension(mappedQuestions));
    }

}
