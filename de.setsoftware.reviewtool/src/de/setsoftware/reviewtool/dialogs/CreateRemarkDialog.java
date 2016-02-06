package de.setsoftware.reviewtool.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.model.RemarkType;
import de.setsoftware.reviewtool.model.ReviewRound;

public class CreateRemarkDialog extends Dialog {

	public interface CreateDialogCallback {
		public abstract void execute(String text, RemarkType type);
	}

	private Combo typeCombo;
	private Text textField;
	private final CreateDialogCallback callback;

	protected CreateRemarkDialog(Shell parentShell, CreateDialogCallback callback) {
		super(parentShell);
		this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
		this.callback = callback;
	}

	public static void get(CreateDialogCallback callback) {
		final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		new CreateRemarkDialog(s, callback).open();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Anmerkung eingeben");
		newShell.setSize(300, 200);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite comp = (Composite) super.createDialogArea(parent);

		final GridLayout layout = (GridLayout) comp.getLayout();
		layout.numColumns = 1;

		this.typeCombo = new Combo(comp, SWT.BORDER | SWT.READ_ONLY);
		this.typeCombo.add(ReviewRound.MUST_FIX_HEADER);
		this.typeCombo.add(ReviewRound.CAN_FIX_HEADER);
		this.typeCombo.add(ReviewRound.ALREADY_FIXED_HEADER);
		this.typeCombo.add(ReviewRound.POSITIVE_HEADER);
		this.typeCombo.add(ReviewRound.TEMPORARY_HEADER);

		this.textField = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.RESIZE);

		final GridData data = new GridData(GridData.FILL_BOTH);
		this.textField.setLayoutData(data);
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

		//f�r noch schnelleres Tippen: Wenn mit einem Buchstaben die Kategorie gew�hlt
		//  wurde springt er direkt in das Textfeld
		this.typeCombo.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent e) {
				for (int i = 0; i < CreateRemarkDialog.this.typeCombo.getItemCount(); i++) {
					if (e.character == CreateRemarkDialog.this.typeCombo.getItem(i).charAt(0)) {
						CreateRemarkDialog.this.typeCombo.select(i);
						CreateRemarkDialog.this.textField.setFocus();
					}
				}
			}
		});

		return comp;
	}

	@Override
	protected void okPressed() {
		if (this.textField.getText().isEmpty()) {
			MessageDialog.openError(this.getShell(), "Anmerkung fehlt",
					"Bitte eine Anmerkung eingeben");
			return;
		}
		if (this.typeCombo.getText().isEmpty()) {
			MessageDialog.openError(this.getShell(), "Art fehlt",
					"Bitte die Art der Anmerkung auswählen");
			return;
		}
		this.callback.execute(
				this.textField.getText(),
				ReviewRound.parseType(this.typeCombo.getText()));
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}
}
