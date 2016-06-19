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
import de.setsoftware.reviewtool.model.EndTransition.Type;
import de.setsoftware.reviewtool.model.ReviewData;
import de.setsoftware.reviewtool.model.ReviewStateManager;

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

    protected EndReviewDialog(
            Shell parentShell,
            ReviewStateManager persistence,
            ReviewData reviewData,
            List<EndTransition> endTransitions) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.persistence = persistence;
        this.reviewData = reviewData;
        this.possibleChoices = endTransitions;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Review beenden - " + this.persistence.getCurrentTicketData().getTicketInfo().getId());
        DialogHelper.restoreSavedSize(newShell, this, 500, 500);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        final Group buttonGroup = new Group(comp, SWT.NONE);
        buttonGroup.setText("Art des Abschlusses");
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

        if (this.reviewData.hasTemporaryMarkers()) {
            this.selectFirstButtonWithType(EndTransition.Type.PAUSE);
        } else if (this.reviewData.hasUnresolvedRemarks()) {
            this.selectFirstButtonWithType(EndTransition.Type.REJECTION);
        } else {
            this.selectFirstButtonWithType(EndTransition.Type.OK);
        }

        this.textField = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.RESIZE);
        this.textField.setText(this.reviewData.serialize());
        this.textField.setLayoutData(new GridData(GridData.FILL_BOTH));

        return comp;
    }

    private void selectFirstButtonWithType(Type type) {
        for (final Button b : this.radioButtons) {
            if (((EndTransition) b.getData()).getType() == type) {
                b.setSelection(true);
                break;
            }
        }
    }

    @Override
    protected void okPressed() {
        for (final Button b : this.radioButtons) {
            if (b.getSelection()) {
                this.typeOfEnd = (EndTransition) b.getData();
                break;
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
            ReviewStateManager persistence, ReviewData reviewData) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        final List<EndTransition> endTransitions = new ArrayList<>();
        endTransitions.add(new EndTransition("Pause", null, EndTransition.Type.PAUSE));
        endTransitions.addAll(persistence.getPossibleTransitionsForReviewEnd());
        final EndReviewDialog dialog =
                new EndReviewDialog(s, persistence, reviewData, endTransitions);
        final int ret = dialog.open();
        if (ret != OK) {
            return null;
        }
        return dialog.typeOfEnd;
    }

}
