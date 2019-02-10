package de.setsoftware.reviewtool.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.model.EndTransition;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.remarks.ReviewData;
import de.setsoftware.reviewtool.ui.api.EndReviewExtension;
import de.setsoftware.reviewtool.ui.api.EndReviewExtensionData;

/**
 * Dialog that is shown before the review is ended and that let's the user select
 * the end transition to use (and so some final adjustments to the review remarks).
 */
public class EndReviewDialog extends Dialog {

    private final ReviewStateManager persistence;
    private final ReviewData reviewData;
    private final List<EndTransition> possibleChoices;
    private List<Button> radioButtons;
    private EndTransition typeOfEnd;
    private Text textField;
    private final List<EndReviewExtension> endReviewExtensions;
    private final List<EndReviewExtensionData> endReviewExtensionData = new ArrayList<>();
    private final EndTransition.Type preferredType;
    private final List<String> namesOfPreferredTransitions;

    protected EndReviewDialog(
            Shell parentShell,
            ReviewStateManager persistence,
            ReviewData reviewData,
            List<EndTransition> endTransitions,
            List<EndReviewExtension> endReviewExtensions,
            EndTransition.Type preferredType,
            List<String> namesOfPreferredTransitions) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.persistence = persistence;
        this.reviewData = reviewData;
        this.possibleChoices = endTransitions;
        this.endReviewExtensions = endReviewExtensions;
        this.preferredType = preferredType;
        this.namesOfPreferredTransitions = namesOfPreferredTransitions;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("End review - " + this.persistence.getCurrentTicketData().getTicketInfo().getId());
        DialogHelper.restoreSavedSize(newShell, this, 500, 700);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        this.textField = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.RESIZE | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
        this.textField.setText(this.reviewData.serialize());
        this.textField.setLayoutData(new GridData(GridData.FILL_BOTH));

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        final Group buttonGroup = new Group(comp, SWT.NONE);
        buttonGroup.setText("Kind of end");
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        buttonGroup.setLayout(gridLayout);
        buttonGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.radioButtons = new ArrayList<>();
        for (final EndTransition t : this.possibleChoices) {
            final Button b = new Button(buttonGroup, SWT.RADIO);
            if (t.getType() != EndTransition.Type.UNKNOWN
                    && t.getType() != EndTransition.Type.PAUSE) {
                b.setText(t.getNameForUser() + " (" + t.getType() + ")");
            } else {
                b.setText(t.getNameForUser());
            }
            b.setData(t);
            this.radioButtons.add(b);
        }

        this.selectPreferredButton();

        for (final EndReviewExtension ext : this.endReviewExtensions) {
            this.endReviewExtensionData.add(ext.createControls(comp));
        }

        return comp;
    }

    private void selectPreferredButton() {
        //select the most preferred one
        for (final String preferred : this.namesOfPreferredTransitions) {
            for (final Button b : this.radioButtons) {
                final EndTransition endTransition = (EndTransition) b.getData();
                if (preferred.equals(endTransition.getNameForUser())) {
                    this.selectAndFocus(b);
                    return;
                }
            }
        }
        //if there was none matching, select the first with the preferred type
        for (final Button b : this.radioButtons) {
            final EndTransition endTransition = (EndTransition) b.getData();
            if (this.preferredType == endTransition.getType()) {
                this.selectAndFocus(b);
                return;
            }
        }
    }

    private void selectAndFocus(Button b) {
        b.setSelection(true);
        b.setFocus();
    }

    @Override
    protected void okPressed() {
        for (final Button b : this.radioButtons) {
            if (b.getSelection()) {
                this.typeOfEnd = (EndTransition) b.getData();
                break;
            }
        }
        for (final EndReviewExtensionData extData : this.endReviewExtensionData) {
            final boolean cancel = extData.okPressed(this.typeOfEnd);
            if (cancel) {
                return;
            }
        }
        this.persistence.saveCurrentReviewData(this.textField.getText());
        DialogHelper.saveDialogSize(this);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }

    /**
     * Lets the use select the type of end transition to use and returns it.
     * If the user decides to continue reviewing, null is returned.
     */
    public static EndTransition selectTypeOfEnd(
            ReviewStateManager persistence,
            ReviewData reviewData,
            List<EndReviewExtension> extensions,
            EndTransition.Type preferredType,
            List<String> namesOfPreferredTransitions) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        final List<EndTransition> endTransitions = new ArrayList<>();
        endTransitions.add(new EndTransition("Pause", null, EndTransition.Type.PAUSE));
        endTransitions.addAll(persistence.getPossibleTransitionsForReviewEnd());

        final EndReviewDialog dialog = new EndReviewDialog(
                s, persistence, reviewData, endTransitions, extensions, preferredType, namesOfPreferredTransitions);
        final int ret = dialog.open();
        if (ret != OK) {
            return null;
        }
        return dialog.typeOfEnd;
    }

}
