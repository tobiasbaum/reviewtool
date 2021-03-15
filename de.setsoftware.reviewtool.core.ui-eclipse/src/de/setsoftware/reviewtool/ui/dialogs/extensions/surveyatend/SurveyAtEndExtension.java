package de.setsoftware.reviewtool.ui.dialogs.extensions.surveyatend;

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import de.setsoftware.reviewtool.ui.api.EndReviewExtension;
import de.setsoftware.reviewtool.ui.api.EndReviewExtensionData;

/**
 * An extension for the end review dialog that adds some questions.
 * The question's answers are then stored using the telemetry provider.
 */
public class SurveyAtEndExtension implements EndReviewExtension {

    private final List<Question> questions;

    public SurveyAtEndExtension(List<Question> questions) {
        this.questions = questions;
    }

    @Override
    public EndReviewExtensionData createControls(Composite comp) {
        return new SurveyAtEndControls(comp, this.questions);
    }

}
