package de.setsoftware.reviewtool.ui.dialogs;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.base.Util;
import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.TicketInfo;

/**
 * Dialog to select a ticket to review. The normal usage is to select the ticket
 * from a list, but it's also possible to directly enter a custom ID.
 */
public class SelectTicketDialog extends Dialog {

    private static final String KEY_LAST_USED_FILTER_REVIEW = "filterReview";
    private static final String KEY_LAST_USED_FILTER_FIXING = "filterFixing";

    private Combo filterCombo;
    private Table selectionTable;
    private Text keyField;

    private final boolean review;
    private final IReviewPersistence persistence;
    private final String oldValue;
    private String selectedKey;

    protected SelectTicketDialog(Shell parentShell, String oldValue, IReviewPersistence p, boolean review) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.persistence = p;
        this.review = review;
        this.oldValue = oldValue;
    }

    /**
     * Shows the dialog and lets the user select a ticket.
     * @return The ticket id, or null if none was selected.
     */
    public static String get(IReviewPersistence p, String oldValue, boolean forReview) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        final SelectTicketDialog dialog = new SelectTicketDialog(s, oldValue, p, forReview);
        final int ret = dialog.open();
        if (ret != OK) {
            return null;
        }
        return dialog.selectedKey;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Choose ticket");
        DialogHelper.restoreSavedSize(newShell, this, 700, 500);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        this.filterCombo = new Combo(comp, SWT.READ_ONLY);
        final Set<String> possibleFilters =
                this.review ? this.persistence.getFilterNamesForReview() : this.persistence.getFilterNamesForFixing();
        this.filterCombo.setItems(possibleFilters.toArray(new String[possibleFilters.size()]));
        this.filterCombo.select(Arrays.asList().indexOf(DialogHelper.getSetting(this.filterKey())));
        if (this.filterCombo.getSelectionIndex() < 0) {
            this.filterCombo.select(0);
        }
        this.filterCombo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                SelectTicketDialog.this.fillTable();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                SelectTicketDialog.this.fillTable();
            }
        });

        this.selectionTable = new Table(comp,
                SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
        this.selectionTable.setHeaderVisible(true);
        this.selectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        final String[] titles = {"ID", "Description", "State", "Prev. reviewers", "Prev. state", "Component"};

        for (int i = 0; i < titles.length; i++) {
            final TableColumn column = new TableColumn(this.selectionTable, SWT.NULL);
            column.setText(titles[i]);
        }

        this.fillTable();

        for (int i = 0; i < titles.length; i++) {
            this.selectionTable.getColumn(i).pack();
        }
        this.selectionTable.setFocus();

        this.keyField = new Text(comp, SWT.BORDER);
        this.keyField.setText(this.oldValue);
        this.keyField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.selectionTable.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                final TableItem item = (TableItem) event.item;
                SelectTicketDialog.this.keyField.setText(item.getText(0));
                SelectTicketDialog.this.okPressed();
            }

            @Override
            public void widgetSelected(SelectionEvent event) {
                final TableItem item = (TableItem) event.item;
                SelectTicketDialog.this.keyField.setText(item.getText(0));
            }
        });

        return comp;
    }

    private String filterKey() {
        return this.review ? KEY_LAST_USED_FILTER_REVIEW : KEY_LAST_USED_FILTER_FIXING;
    }

    private void fillTable() {
        this.selectionTable.removeAll();

        for (final TicketInfo ticket : this.loadTickets()) {
            final TableItem item = new TableItem(this.selectionTable, SWT.NULL);
            item.setText(0, ticket.getId());
            item.setText(1, ticket.getSummaryIncludingParent());
            item.setText(2, ticket.getState());
            item.setText(3, Util.implode(ticket.getReviewers()));
            item.setText(4, ticket.getPreviousState());
            item.setText(5, ticket.getComponent());
        }
    }

    private List<TicketInfo> loadTickets() {
        return this.persistence.getTicketsForFilter(this.filterCombo.getText());
    }

    @Override
    protected void okPressed() {
        if (this.keyField.getText().isEmpty()) {
            MessageDialog.openError(this.getShell(), "ID missing",
                    "Please enter a ticket ID or select a ticket.");
            return;
        }
        this.selectedKey = this.keyField.getText();
        DialogHelper.saveDialogSize(this);
        DialogHelper.saveSetting(this.filterKey(), this.filterCombo.getText());
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }

}
