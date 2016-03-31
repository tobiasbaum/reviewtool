package de.setsoftware.reviewtool.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.TicketInfo;

public class ReviewInfoDialog extends Dialog {

    private final ITicketData ticketData;

    protected ReviewInfoDialog(Shell parentShell, ITicketData ticketData) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.ticketData = ticketData;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Review von Ticket " + this.ticketData.getTicketInfo().getId());
        DialogHelper.restoreSavedSize(newShell, this, 500, 500);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        final TicketInfo ticketInfo = this.ticketData.getTicketInfo();
        this.createLabelAndText(comp, "Ticket-Schlüssel:", ticketInfo.getId(), SWT.SINGLE);
        this.createLabelAndText(comp, "Titel:", ticketInfo.getSummary(), SWT.SINGLE | SWT.WRAP);
        this.createLabelAndText(comp, "Reviewanmerkungen:", this.ticketData.getReviewData(), SWT.MULTI);

        return comp;
    }

    private void createLabelAndText(Composite comp, String labelText, String text, int style) {
        final Label label = new Label(comp, SWT.NULL);
        label.setText(labelText);

        final Text field = new Text(comp, style | SWT.BORDER | SWT.RESIZE);
        field.setText(text);
        field.setLayoutData(new GridData(GridData.FILL_BOTH));
        field.setEditable(false);
    }

    public static void show(ReviewStateManager persistence) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final ReviewInfoDialog dialog = new ReviewInfoDialog(s, persistence.getCurrentTicketData());
        dialog.open();
    }


    @Override
    protected void okPressed() {
        DialogHelper.saveDialogSize(this);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }
}
