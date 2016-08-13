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
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.Tour;

/**
 * A dialog that let's the user select one of several tour structures.
 */
public class SelectTourStructureDialog extends Dialog {

    private final List<? extends Pair<String, List<? extends Tour>>> choices;
    private List<Button> radioButtons;
    private List<? extends Tour> chosenTours;

    protected SelectTourStructureDialog(
            Shell parentShell,
            List<? extends Pair<String, List<? extends Tour>>> choices) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.choices = choices;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Choose tour structure");
        DialogHelper.restoreSavedSize(newShell, this, 500, 400);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        final Group buttonGroup = new Group(comp, SWT.NONE);
        buttonGroup.setText("Structure to use");
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        buttonGroup.setLayout(gridLayout);
        buttonGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.radioButtons = new ArrayList<>();
        for (final Pair<String, List<? extends Tour>> choice : this.choices) {
            final Button b = new Button(buttonGroup, SWT.RADIO);
            b.setText(this.createText(choice));
            b.setData(choice.getSecond());
            this.radioButtons.add(b);
        }

        this.selectButtonWithBestStructure();

        return comp;
    }

    private String createText(Pair<String, List<? extends Tour>> choice) {
        return String.format("%s (%d stops in %d tours)",
                choice.getFirst(),
                this.countStops(choice.getSecond()),
                choice.getSecond().size());
    }

    private void selectButtonWithBestStructure() {
        //the "best" structure is the one with the fewest stops, and of these the one with
        //  the most tours (hoping that the additional tours help in understanding)
        int bestStopCount = Integer.MAX_VALUE;
        int bestTourCount = Integer.MIN_VALUE;
        Button bestButton = null;

        for (final Button b : this.radioButtons) {
            final List<? extends Tour> toursInStructure = (List<? extends Tour>) b.getData();
            final int curStopCount = this.countStops(toursInStructure);
            final int curTourCount = toursInStructure.size();
            if (curStopCount < bestStopCount
                    || (curStopCount == bestStopCount && curTourCount > bestTourCount)) {
                bestStopCount = curStopCount;
                bestTourCount = curTourCount;
                bestButton = b;
            }
        }
        bestButton.setSelection(true);
    }

    private int countStops(List<? extends Tour> toursInStructure) {
        int count = 0;
        for (final Tour t : toursInStructure) {
            count += t.getNumberOfStops(false);
        }
        return count;
    }

    @Override
    protected void okPressed() {
        for (final Button b : this.radioButtons) {
            if (b.getSelection()) {
                this.chosenTours = (List<? extends Tour>) b.getData();
                break;
            }
        }
        DialogHelper.saveDialogSize(this);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }

    /**
     * Lets the user select the structure he wants to use.
     * When the user cancels, null is returned.
     */
    public static List<? extends Tour> selectStructure(
            List<? extends Pair<String, List<? extends Tour>>> choices) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        final SelectTourStructureDialog dialog =
                new SelectTourStructureDialog(s, choices);
        final int ret = dialog.open();
        if (ret != OK) {
            return null;
        }
        return dialog.chosenTours;
    }

}
