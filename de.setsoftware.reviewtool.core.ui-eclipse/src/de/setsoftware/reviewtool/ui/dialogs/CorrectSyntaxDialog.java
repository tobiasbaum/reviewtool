package de.setsoftware.reviewtool.ui.dialogs;

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

import de.setsoftware.reviewtool.model.InMemoryReviewDataCache;
import de.setsoftware.reviewtool.model.PersistenceStub;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.StubUi;
import de.setsoftware.reviewtool.model.remarks.DummyMarker;
import de.setsoftware.reviewtool.model.remarks.FileLinePosition;
import de.setsoftware.reviewtool.model.remarks.FilePosition;
import de.setsoftware.reviewtool.model.remarks.GlobalPosition;
import de.setsoftware.reviewtool.model.remarks.IMarkerFactory;
import de.setsoftware.reviewtool.model.remarks.IReviewMarker;
import de.setsoftware.reviewtool.model.remarks.RemarkType;
import de.setsoftware.reviewtool.model.remarks.ResolutionType;
import de.setsoftware.reviewtool.model.remarks.ReviewData;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;

/**
 * Dialog that allows the user to manually correct the syntax of review data.
 */
public class CorrectSyntaxDialog extends Dialog {

    private final String errorMessage;
    private final String oldReviewData;
    private Text textField;
    private String correctedText;
    private final String ticketKey;

    protected CorrectSyntaxDialog(Shell parentShell, String errorMessage, String reviewData, String ticketKey) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.errorMessage = errorMessage;
        this.oldReviewData = reviewData;
        this.ticketKey = ticketKey;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Syntax error correction, " + this.ticketKey);
        DialogHelper.restoreSavedSize(newShell, this, 500, 700);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        final Label label = new Label(comp, SWT.NULL);
        label.setText("The review data has syntax errors. Please correct manually:\n\n"
                + this.errorMessage);

        this.textField = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.RESIZE | SWT.H_SCROLL | SWT.V_SCROLL);
        this.textField.setText(this.oldReviewData);
        this.textField.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Label example = new Label(comp, SWT.NULL);
        example.setText("Example:\n\n" + this.createExampleData());

        return comp;
    }

    private String createExampleData() {
        try {
            final PersistenceStub s = new PersistenceStub();
            final InMemoryReviewDataCache c = new InMemoryReviewDataCache();
            final ReviewStateManager p = new ReviewStateManager(c, s, new StubUi("TEST-1234"));
            final ReviewRemark r1 = ReviewRemark.create(
                    newMarker(), "TB", new GlobalPosition(),
                    "global review remark important to the reviewer", RemarkType.MUST_FIX);
            r1.addComment("AUTHOR-ID", "Nachfrage");
            r1.setResolution(ResolutionType.QUESTION);
            p.saveRemark(r1);
            final ReviewRemark r2 = ReviewRemark.create(
                    newMarker(), "TB", new FilePosition("FileName"),
                    "optional remark, with reference to a file", RemarkType.CAN_FIX);
            r2.addComment("AUTHOR-ID", "comment to refuse fixing");
            r2.setResolution(ResolutionType.WONT_FIX);
            p.saveRemark(r2);
            final ReviewRemark r3 = ReviewRemark.create(
                    newMarker(), "TB", new FileLinePosition("FileName", 42),
                    "remark for direct fixing in a certain line", RemarkType.ALREADY_FIXED);
            p.saveRemark(r3);

            s.setReviewRound(2);
            final ReviewRemark r4 = ReviewRemark.create(
                    newMarker(), "TB", new GlobalPosition(),
                    "well done", RemarkType.POSITIVE);
            p.saveRemark(r4);
            final ReviewRemark r5 = ReviewRemark.create(
                    newMarker(), "TB", new GlobalPosition(),
                    "temporary marker for the reviewer", RemarkType.TEMPORARY);
            p.saveRemark(r5);
            final ReviewRemark r6 = ReviewRemark.create(
                    newMarker(), "TB", new GlobalPosition(),
                    "some other remark, e.g. 'part of the remarks have been communicated orally'", RemarkType.OTHER);
            p.saveRemark(r6);

            return p.getCurrentReviewData();
        } catch (final ReviewRemarkException e) {
            throw new AssertionError(e);
        }
    }

    private static IReviewMarker newMarker() {
        return new DummyMarker();
    }

    @Override
    protected void okPressed() {
        this.correctedText = this.textField.getText();
        DialogHelper.saveDialogSize(this);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }

    private static String allowCorrection(String errorMessage, String reviewData, String ticketKey) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final CorrectSyntaxDialog dialog = new CorrectSyntaxDialog(s, errorMessage, reviewData, ticketKey);
        final int ret = dialog.open();
        if (ret != OK) {
            return null;
        }
        return dialog.correctedText;
    }

    /**
     * Loads the current review data. If it contains syntax errors, the user can correct these. If he fails to
     * do so or cancels for another reason, null is returned.
     */
    public static ReviewData getCurrentReviewDataParsed(ReviewStateManager persistence, IMarkerFactory factory) {
        String reviewData = persistence.getCurrentReviewData();
        if (reviewData == null) {
            return null;
        }
        String parseError;
        boolean hadError = false;
        while ((parseError = canBeParsed(persistence, reviewData)) != null) {
            hadError = true;
            reviewData = allowCorrection(parseError, reviewData, persistence.getTicketKey());
            if (reviewData == null) {
                return null;
            }
        }
        if (hadError) {
            persistence.saveCurrentReviewData(reviewData);
        }
        return ReviewData.parse(persistence.getReviewersForRounds(), factory, reviewData);
    }

    private static String canBeParsed(ReviewStateManager persistence, String reviewData) {
        try {
            ReviewData.parse(persistence.getReviewersForRounds(), DummyMarker.FACTORY, reviewData);
            return null;
        } catch (final RuntimeException e) {
            return e.getMessage() == null ? e.toString() : e.getMessage();
        }
    }

}
