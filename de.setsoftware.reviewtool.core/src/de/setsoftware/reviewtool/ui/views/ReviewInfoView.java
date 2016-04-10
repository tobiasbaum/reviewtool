package de.setsoftware.reviewtool.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.TicketInfo;

/**
 * A view that contains general information on the review and the ticket.
 */
public class ReviewInfoView extends ViewPart implements ReviewModeListener {

    private Composite comp;

    @Override
    public void createPartControl(Composite comp) {
        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        comp.setLayout(layout);

        this.comp = comp;

        ViewDataSource.get().registerListener(this);
    }

    private void createLabelAndText(Composite comp, String labelText, String text, int style) {
        final Label label = new Label(comp, SWT.NULL);
        label.setText(labelText);

        final Text field = new Text(comp, style | SWT.BORDER | SWT.RESIZE);
        field.setText(text);
        field.setLayoutData(new GridData(GridData.FILL_BOTH));
        field.setEditable(false);
    }

    @Override
    public void setFocus() {
        // TODO Auto-generated method stub

    }

    @Override
    public void notifyReview(ReviewStateManager mgr) {
        final ITicketData ticketData = mgr.getCurrentTicketData();
        final TicketInfo ticketInfo = ticketData.getTicketInfo();
        this.createLabelAndText(this.comp, "Ticket-Schlüssel:", ticketInfo.getId(), SWT.SINGLE);
        this.createLabelAndText(this.comp, "Titel:", ticketInfo.getSummary(), SWT.SINGLE | SWT.WRAP);
        this.createLabelAndText(this.comp, "Reviewanmerkungen:", ticketData.getReviewData(), SWT.MULTI);
    }

    @Override
    public void notifyFixing(ReviewStateManager mgr) {
        // TODO Auto-generated method stub

    }

    @Override
    public void notifyIdle() {
        // TODO Auto-generated method stub

    }

}
