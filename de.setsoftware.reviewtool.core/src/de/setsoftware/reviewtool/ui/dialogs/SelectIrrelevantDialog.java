package de.setsoftware.reviewtool.ui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.Change;

/**
 * A dialog that let's the user choose which of several filters that mark changes as irrelevant to apply.
 */
public class SelectIrrelevantDialog extends Dialog {

    private final List<? extends Pair<String, Set<? extends Change>>> choices;
    private List<Button> checkboxes;
    private List<Pair<String, Set<? extends Change>>> chosenSubset;

    protected SelectIrrelevantDialog(
            Shell parentShell,
            List<? extends Pair<String, Set<? extends Change>>> choices) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.choices = choices;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Choose change filters to apply");
        DialogHelper.restoreSavedSize(newShell, this, 500, 400);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        final Group buttonGroup = new Group(comp, SWT.NONE);
        buttonGroup.setText("Filters to apply");
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        buttonGroup.setLayout(gridLayout);
        buttonGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.checkboxes = new ArrayList<>();
        for (final Pair<String, Set<? extends Change>> choice : this.choices) {
            final Button b = new Button(buttonGroup, SWT.CHECK);
            b.setText(this.createText(choice));
            b.setData(choice);
            this.checkboxes.add(b);
        }

        this.restoreSavedSelection();

        return comp;
    }

    private String createText(Pair<String, Set<? extends Change>> choice) {
        return String.format("%s (applies to %d changes)",
                choice.getFirst(),
                choice.getSecond().size());
    }

    private void restoreSavedSelection() {
        for (final Button b : this.checkboxes) {
            b.setSelection(this.getSavedSelectionState((Pair<String, Set<? extends Change>>) b.getData()));
        }
    }

    @Override
    protected void okPressed() {
        this.chosenSubset = new ArrayList<>();
        for (final Button b : this.checkboxes) {
            final Pair<String, Set<? extends Change>> data = (Pair<String, Set<? extends Change>>) b.getData();
            if (b.getSelection()) {
                this.chosenSubset.add(data);
            }
            this.saveSelectionState(data, b.getSelection());
        }
        DialogHelper.saveDialogSize(this);
        super.okPressed();
    }

    private boolean getSavedSelectionState(Pair<String, Set<? extends Change>> data) {
        return Boolean.parseBoolean(DialogHelper.getSetting(this.makeSettingId(data)));
    }

    private void saveSelectionState(Pair<String, Set<? extends Change>> data, boolean selection) {
        DialogHelper.saveSetting(this.makeSettingId(data), Boolean.toString(selection));
    }

    private String makeSettingId(Pair<String, Set<? extends Change>> data) {
        return data.getFirst().replaceAll("[^a-zA-Z0-9]", "");
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }

    /**
     * Lets the user select the filters he wants to apply.
     * When the user cancels, null is returned.
     */
    public static List<? extends Pair<String, Set<? extends Change>>> selectIrrelevant(
            List<? extends Pair<String, Set<? extends Change>>> choices) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        final SelectIrrelevantDialog dialog =
                new SelectIrrelevantDialog(s, choices);
        final int ret = dialog.open();
        if (ret != OK) {
            return null;
        }
        return dialog.chosenSubset;
    }

}
