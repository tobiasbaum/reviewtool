package de.setsoftware.reviewtool.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.model.IReviewPersistence;
import de.setsoftware.reviewtool.model.TicketInfo;

public class SelectTicketDialog extends Dialog {

	private Table selectionTable;
	private Text keyField;

	private final List<TicketInfo> tickets;
	private final String oldValue;
	private String selectedKey;

	protected SelectTicketDialog(Shell parentShell, String oldValue, List<TicketInfo> tickets) {
		super(parentShell);
		this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
		this.oldValue = oldValue;
		this.tickets = tickets;
	}

	public static String get(IReviewPersistence p, String oldValue, boolean forReview) {
		final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final SelectTicketDialog dialog = new SelectTicketDialog(
				s, oldValue, forReview ? p.getReviewableTickets() : p.getFixableTickets());
		final int ret = dialog.open();
		if (ret != OK) {
			return null;
		}
		return dialog.selectedKey;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Ticket auswählen");
		newShell.setSize(400, 500);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite comp = (Composite) super.createDialogArea(parent);

		final GridLayout layout = (GridLayout) comp.getLayout();
		layout.numColumns = 1;

		this.selectionTable = new Table(comp,
				SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		this.selectionTable.setHeaderVisible(true);
		this.selectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		final String[] titles = {"ID", "Beschreibung", "Status", "Komponente"};

		for (int i = 0; i < titles.length; i++) {
			final TableColumn column = new TableColumn(this.selectionTable, SWT.NULL);
			column.setText(titles[i]);
		}

		for (final TicketInfo ticket : this.tickets) {
			final TableItem item = new TableItem(this.selectionTable, SWT.NULL);
			item.setText(0, ticket.getID());
			item.setText(1, ticket.getDescription());
			item.setText(2, ticket.getState());
			item.setText(3, ticket.getComponent());
		}

		for (int i = 0; i < titles.length; i++) {
			this.selectionTable.getColumn(i).pack();
		}

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
			public void widgetSelected(SelectionEvent arg0) { }
		});

		return comp;
	}

	@Override
	protected void okPressed() {
		if (this.keyField.getText().isEmpty()) {
			MessageDialog.openError(this.getShell(), "Schlüssel fehlt",
					"Bitte einen Ticket-Schlüssel eingeben");
			return;
		}
		this.selectedKey = this.keyField.getText();
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}

}
