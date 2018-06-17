package de.setsoftware.reviewtool.ui.dialogs.extensions.surveyatend;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.EndTransition;
import de.setsoftware.reviewtool.model.EndTransition.Type;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.telemetry.TelemetryEventBuilder;
import de.setsoftware.reviewtool.ui.dialogs.extensions.EndReviewExtensionData;

/**
 * The combo boxes for the survey in the end review dialog.
 */
public class SurveyAtEndControls implements EndReviewExtensionData {

    private final List<Pair<Question, Combo>> combos = new ArrayList<>();

    public SurveyAtEndControls(Composite comp, List<Question> questions) {
        for (final Question q : questions) {
            final Label label = new Label(comp, 0);
            label.setText(q.getText());
            final Combo combo = new Combo(comp, SWT.READ_ONLY);
            for (final Answer a : q.getChoices()) {
                combo.add(a.getText());
            }
            this.combos.add(Pair.create(q, combo));
        }
    }

    @Override
    public boolean okPressed(EndTransition typeOfEnd) {
        if (typeOfEnd.getType() == Type.PAUSE) {
            return false;
        }

        final TelemetryEventBuilder event = Telemetry.event("surveyResult");
        for (final Pair<Question, Combo> combo : this.combos) {
            final String answerId = combo.getFirst().getIdForChoiceText(combo.getSecond().getText());
            if (answerId == null) {
                MessageDialog.openError(null, "Please fill in survey",
                        "Please answer the survey questions shown in the dialog.");
                return true;
            }
            event.param("q_" + combo.getFirst().getId(), answerId);
        }
        event.log();
        return false;
    }

}
