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

import de.setsoftware.reviewtool.model.DummyMarker;
import de.setsoftware.reviewtool.model.IMarkerFactory;
import de.setsoftware.reviewtool.model.ReviewData;
import de.setsoftware.reviewtool.model.ReviewStateManager;

public class CorrectSyntaxDialog extends Dialog {

	private final String errorMessage;
	private final String oldReviewData;
	private Text textField;
	private String correctedText;

	protected CorrectSyntaxDialog(Shell parentShell, String errorMessage, String reviewData) {
		super(parentShell);
		this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
		this.errorMessage = errorMessage;
		this.oldReviewData = reviewData;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Syntaxfehler-Korrektur");
		newShell.setSize(500, 500);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite comp = (Composite) super.createDialogArea(parent);

		final GridLayout layout = (GridLayout) comp.getLayout();
		layout.numColumns = 1;

		final Label label = new Label(comp, 0);
		label.setText("Die Review-Daten sind syntaktisch fehlerhaft. Bitte manuell korrigieren:\n\n"
				+ this.errorMessage);

		this.textField = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.RESIZE);
		this.textField.setText(this.oldReviewData);
		this.textField.setLayoutData(new GridData(GridData.FILL_BOTH));

		return comp;
	}

	@Override
	protected void okPressed() {
		this.correctedText = this.textField.getText();
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}

	private static String allowCorrection(String errorMessage, String reviewData) {
		final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final CorrectSyntaxDialog dialog = new CorrectSyntaxDialog(s, errorMessage, reviewData);
		final int ret = dialog.open();
		if (ret != OK) {
			return null;
		}
		return dialog.correctedText;
	}

	public static ReviewData getCurrentReviewDataParsed(ReviewStateManager persistence, IMarkerFactory factory) {
		String reviewData = persistence.getCurrentReviewData();
		if (reviewData == null) {
			return null;
		}
		String parseError;
		boolean hadError = false;
		while ((parseError = canBeParsed(persistence, reviewData)) != null) {
			hadError = true;
			reviewData = allowCorrection(parseError, reviewData);
			if (reviewData == null) {
				return null;
			}
		}
		if (hadError) {
			persistence.saveCurrentReviewData(reviewData);
		}
		return ReviewData.parse(persistence, factory, reviewData);
	}

	private static String canBeParsed(ReviewStateManager persistence, String reviewData) {
		try {
			ReviewData.parse(persistence, DummyMarker.FACTORY, reviewData);
			return null;
		} catch (final RuntimeException e) {
			return e.getMessage() == null ? e.toString() : e.getMessage();
		}
	}

}
