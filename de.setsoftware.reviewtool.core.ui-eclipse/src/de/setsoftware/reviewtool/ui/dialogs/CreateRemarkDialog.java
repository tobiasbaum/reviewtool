package de.setsoftware.reviewtool.ui.dialogs;

import java.util.Set;

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

import de.setsoftware.reviewtool.model.api.PositionReference;
import de.setsoftware.reviewtool.model.remarks.RemarkType;
import de.setsoftware.reviewtool.model.remarks.ReviewRound;

/**
 * Dialog for the creation of review remarks.
 */
public class CreateRemarkDialog extends Dialog {

    private static final String LINE_TEXT = "Reference: Line";
    private static final String FILE_TEXT = "Reference: File";
    private static final String GLOBAL_TEXT = "Reference: global";

    /**
     * Callback that is called when the user entered the remark's information.
     */
    public interface CreateDialogCallback {
        public abstract void execute(String text, RemarkType type, PositionReference referenceToUse);
    }

    private Combo typeCombo;
    private Text textField;
    private Combo positionCombo;
    private final CreateDialogCallback callback;
    private final Set<PositionReference> possibleReferences;
    private final String prefillText;

    protected CreateRemarkDialog(
            Shell parentShell,
            CreateDialogCallback callback,
            Set<PositionReference> possibleReferences,
            String prefillText) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.callback = callback;
        this.possibleReferences = possibleReferences;
        this.prefillText = prefillText;
    }

    public static void get(
            Set<PositionReference> possibleReferences, String prefillText, CreateDialogCallback callback) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        new CreateRemarkDialog(s, callback, possibleReferences, prefillText).open();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Enter remark");
        newShell.setSize(300, 270);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        this.typeCombo = new Combo(comp, SWT.BORDER | SWT.READ_ONLY);
        this.typeCombo.add(ReviewRound.MUST_FIX_HEADER[0]);
        this.typeCombo.add(ReviewRound.CAN_FIX_HEADER[0]);
        this.typeCombo.add(ReviewRound.ALREADY_FIXED_HEADER[0]);
        this.typeCombo.add(ReviewRound.POSITIVE_HEADER[0]);
        this.typeCombo.add(ReviewRound.TEMPORARY_HEADER[0]);
        this.typeCombo.add(ReviewRound.OTHER_REMARK_HEADER[0]);

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
        this.textField.setText(this.prefillText);

        this.positionCombo = new Combo(comp, SWT.BORDER | SWT.READ_ONLY);
        if (this.possibleReferences.contains(PositionReference.LINE)) {
            this.positionCombo.add(LINE_TEXT);
        }
        if (this.possibleReferences.contains(PositionReference.FILE)) {
            this.positionCombo.add(FILE_TEXT);
        }
        if (this.possibleReferences.contains(PositionReference.GLOBAL)) {
            this.positionCombo.add(GLOBAL_TEXT);
        }
        this.positionCombo.select(0);

        //für noch schnelleres Tippen: Wenn mit einem Buchstaben die Kategorie gewählt
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
            MessageDialog.openError(this.getShell(), "Remark empty",
                    "Please enter a text for the remark");
            return;
        }
        if (this.typeCombo.getText().isEmpty()) {
            MessageDialog.openError(this.getShell(), "Kind missing",
                    "Please choose the kind of remark");
            return;
        }
        this.callback.execute(
                this.textField.getText(),
                ReviewRound.parseType(this.typeCombo.getText()),
                this.getSelectedPosition());
        DialogHelper.saveDialogSize(this);
        super.okPressed();
    }

    private PositionReference getSelectedPosition() {
        final String s = this.positionCombo.getText();
        if (s.equals(LINE_TEXT)) {
            return PositionReference.LINE;
        } else if (s.equals(FILE_TEXT)) {
            return PositionReference.FILE;
        } else {
            return PositionReference.GLOBAL;
        }
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }
}
