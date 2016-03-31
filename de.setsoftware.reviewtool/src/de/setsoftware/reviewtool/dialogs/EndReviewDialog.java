package de.setsoftware.reviewtool.dialogs;

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

import de.setsoftware.reviewtool.model.ReviewData;
import de.setsoftware.reviewtool.model.ReviewStateManager;

public class EndReviewDialog extends Dialog {

    public enum TypeOfEnd {
        PAUSE,
        REJECTED,
        OK,
        ANOTHER_REVIEW
    }

    private final ReviewStateManager persistence;
    private final ReviewData reviewData;
    private Button buttonPause;
    private Button buttonOk;
    private Button buttonRejected;
    private Button buttonAnotherReview;
    private TypeOfEnd typeOfEnd;
    private Text textField;

    protected EndReviewDialog(Shell parentShell, ReviewStateManager persistence, ReviewData reviewData) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.persistence = persistence;
        this.reviewData = reviewData;
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

        this.buttonPause = new Button(buttonGroup, SWT.RADIO);
        this.buttonPause.setText("Pause");
        this.buttonOk = new Button(buttonGroup, SWT.RADIO);
        this.buttonOk.setText("OK");
        this.buttonRejected = new Button(buttonGroup, SWT.RADIO);
        this.buttonRejected.setText("Rückläufer");
        this.buttonAnotherReview = new Button(buttonGroup, SWT.RADIO);
        this.buttonAnotherReview.setText("Weiteres Review nötig");

        if (this.reviewData.hasTemporaryMarkers()) {
            this.buttonPause.setSelection(true);
        } else if (this.reviewData.hasUnresolvedRemarks()) {
            this.buttonRejected.setSelection(true);
        } else {
            this.buttonOk.setSelection(true);
        }

        this.textField = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.RESIZE);
        this.textField.setText(this.reviewData.serialize());
        this.textField.setLayoutData(new GridData(GridData.FILL_BOTH));

        return comp;
    }

    @Override
    protected void okPressed() {
        if (this.buttonPause.getSelection()) {
            this.typeOfEnd = TypeOfEnd.PAUSE;
        } else if (this.buttonOk.getSelection()) {
            this.typeOfEnd = TypeOfEnd.OK;
        } else if (this.buttonRejected.getSelection()) {
            this.typeOfEnd = TypeOfEnd.REJECTED;
        } else {
            this.typeOfEnd = TypeOfEnd.ANOTHER_REVIEW;
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

    public static TypeOfEnd selectTypeOfEnd(ReviewStateManager persistence, ReviewData reviewData) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final EndReviewDialog dialog = new EndReviewDialog(s, persistence, reviewData);
        final int ret = dialog.open();
        if (ret != OK) {
            return null;
        }
        return dialog.typeOfEnd;
    }

}
