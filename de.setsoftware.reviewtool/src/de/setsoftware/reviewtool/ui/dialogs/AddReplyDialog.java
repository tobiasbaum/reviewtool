package de.setsoftware.reviewtool.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.model.ReviewRemarkComment;

public class AddReplyDialog extends Dialog {

    private Text textField;
    private final ReviewRemark review;
    private final InputDialogCallback callback;

    protected AddReplyDialog(Shell parentShell, ReviewRemark review, InputDialogCallback callback) {
        super(parentShell);
        this.review = review;
        this.callback = callback;
    }

    public static void get(ReviewRemark review, InputDialogCallback callback) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        new AddReplyDialog(s, review, callback).open();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Anmerkung eingeben");
        DialogHelper.restoreSavedSize(newShell, this, 300, 200);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        final Font italicFont = JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);

        int i = 0;
        for (final ReviewRemarkComment comment : this.review.getComments()) {
            final Label oldDiscussionLabel = new Label(comp, SWT.LEFT);
            oldDiscussionLabel.setText(comment.toString());
            if (i % 2 != 0) {
                oldDiscussionLabel.setFont(italicFont);
            }
            i++;
        }

        this.textField = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.WRAP);

        final GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        this.textField.setLayoutData(data);
        this.textField.setSize(250, 50);
        this.textField.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT
                        || e.detail == SWT.TRAVERSE_TAB_PREVIOUS
                        || e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = true;
                }
            }
        });

        return comp;
    }

    @Override
    protected void okPressed() {
        this.callback.execute(this.textField.getText());
        DialogHelper.saveDialogSize(this);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }
}
