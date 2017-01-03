package de.setsoftware.reviewtool.ui.views;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.IReviewDataSaveListener;
import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.TicketInfo;
import de.setsoftware.reviewtool.model.TicketLinkSettings;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.ui.dialogs.RemarkMarkers;

/**
 * A view that contains general information on the review and the ticket.
 */
public class ReviewInfoView extends ViewPart implements ReviewModeListener, IReviewDataSaveListener {

    private Composite comp;
    private Composite currentContent;
    private Text reviewDataText;

    private String lastText = "";
    private String ticketId;

    @Override
    public void createPartControl(Composite comp) {
        this.comp = comp;

        ViewDataSource.get().registerListener(this);
    }

    private Text createIdStuff(Composite comp, String labelText, final String id, ReviewStateManager mgr) {
        final Label label = new Label(comp, SWT.NULL);
        label.setText(labelText);
        ViewHelper.createContextMenuWithoutSelectionProvider(this, label);

        final Composite textfieldAndButtons = new Composite(comp, SWT.NULL);
        final RowLayout layout = new RowLayout();
        textfieldAndButtons.setLayout(layout);
        textfieldAndButtons.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Text field = new Text(textfieldAndButtons,
                SWT.SINGLE | SWT.BORDER | SWT.RESIZE | SWT.H_SCROLL | SWT.V_SCROLL);
        field.setText(id + ", round " + mgr.getCurrentRound());
        field.setEditable(false);
        ViewHelper.createContextMenuWithoutSelectionProvider(this, field);

        final TicketLinkSettings linkSettings = mgr.getTicketLinkSettings();
        if (linkSettings != null) {
            final Button openButton = new Button(textfieldAndButtons, SWT.NULL);
            openButton.setText(linkSettings.getText());

            openButton.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent ev) {
                    try {
                        Desktop.getDesktop().browse(new URI(linkSettings.createLinkFor(id)));
                    } catch (IOException | URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent ev) {
                    this.widgetSelected(ev);
                }
            });
        }

        final Button copyButton = new Button(textfieldAndButtons, SWT.NULL);
        copyButton.setText("Copy ID to clipboard");
        copyButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
                final Clipboard cb = new Clipboard(Display.getCurrent());
                cb.setContents(new Object[] {id}, new Transfer[] {TextTransfer.getInstance()});
                cb.dispose();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent ev) {
                this.widgetSelected(ev);
            }
        });

        return field;
    }

    private Text createLabelAndText(Composite comp, String labelText, String text, int style, int fill) {
        final Label label = new Label(comp, SWT.NULL);
        label.setText(labelText);
        ViewHelper.createContextMenuWithoutSelectionProvider(this, label);

        final Text field = new Text(comp, style | SWT.BORDER | SWT.RESIZE | SWT.H_SCROLL | SWT.V_SCROLL);
        field.setText(text);
        field.setLayoutData(new GridData(fill));
        field.setEditable(false);
        ViewHelper.createContextMenuWithoutSelectionProvider(this, field);

        return field;
    }

    @Override
    public void setFocus() {
        if (this.reviewDataText != null) {
            this.reviewDataText.setFocus();
        }
    }

    @Override
    public void notifyReview(ReviewStateManager mgr, ToursInReview tours) {
        mgr.addSaveListener(this);
        this.disposeOldContent();
        if (this.comp.isDisposed()) {
            return;
        }
        this.currentContent = this.createReviewContent(mgr);
        this.comp.layout();
    }

    private ScrolledComposite createReviewContent(ReviewStateManager mgr) {
        return this.createCommonContentForReviewAndFixing(mgr, "Review active for:");
    }

    private ScrolledComposite createCommonContentForReviewAndFixing(final ReviewStateManager mgr, String title) {
        final ScrolledComposite scroll = new ScrolledComposite(this.comp, SWT.VERTICAL);
        scroll.setExpandHorizontal(true);
        scroll.setExpandVertical(true);
        scroll.setMinSize(300, 300);

        final Composite scrollContent = new Composite(scroll, SWT.NULL);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        scrollContent.setLayout(layout);

        final ITicketData ticketData = mgr.getCurrentTicketData();
        final TicketInfo ticketInfo = ticketData.getTicketInfo();
        this.ticketId = ticketInfo.getId();
        this.createIdStuff(scrollContent, title, ticketInfo.getId(), mgr);
        this.createLabelAndText(scrollContent, "Title:", ticketInfo.getSummary(),
                SWT.SINGLE | SWT.WRAP, GridData.FILL_HORIZONTAL);
        this.reviewDataText = this.createLabelAndText(scrollContent, "Review remarks:", ticketData.getReviewData(),
                SWT.MULTI, GridData.FILL_BOTH);
        this.lastText = this.reviewDataText.getText();
        this.reviewDataText.setEditable(true);
        this.reviewDataText.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                final String curText = ReviewInfoView.this.reviewDataText.getText();
                if (!curText.equals(ReviewInfoView.this.lastText)) {
                    ReviewInfoView.this.handleReviewDataTextChanged(mgr, curText);
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
            }
        });

        scroll.setContent(scrollContent);
        return scroll;
    }

    private void handleReviewDataTextChanged(ReviewStateManager mgr, String newReviewData) {
        try {
            Logger.debug("change in review remark text");
            mgr.saveCurrentReviewData(newReviewData);
            RemarkMarkers.clearMarkers();
            RemarkMarkers.loadRemarks(mgr);
            this.lastText = newReviewData;
        } catch (final CoreException e) {
            throw new ReviewtoolException(e);
        }
    }

    @Override
    public void notifyFixing(ReviewStateManager mgr) {
        mgr.addSaveListener(this);
        this.disposeOldContent();
        if (this.comp.isDisposed()) {
            return;
        }
        this.currentContent = this.createFixingContent(mgr);
        this.comp.layout();
    }

    private Composite createFixingContent(ReviewStateManager mgr) {
        return this.createCommonContentForReviewAndFixing(mgr, "Fixing active for:");
    }

    @Override
    public void notifyIdle() {
        this.disposeOldContent();
        if (this.comp.isDisposed()) {
            return;
        }
        this.currentContent = this.createIdleContent();
        this.comp.layout();
    }

    private Composite createIdleContent() {
        this.reviewDataText = null;

        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());
        final Label label = new Label(panel, SWT.NULL);
        if (this.ticketId == null) {
            label.setText("No review or fixing active");
        } else {
            label.setText("No review or fixing active. Last finished: " + this.ticketId);
        }

        ViewHelper.createContextMenuWithoutSelectionProvider(this, label);

        return panel;
    }

    private void disposeOldContent() {
        if (this.currentContent != null) {
            this.currentContent.dispose();
        }
    }

    @Override
    public void onSave(String newData) {
        if (this.reviewDataText != null && !this.reviewDataText.isDisposed()) {
            this.reviewDataText.setText(newData);
        }
    }

}
