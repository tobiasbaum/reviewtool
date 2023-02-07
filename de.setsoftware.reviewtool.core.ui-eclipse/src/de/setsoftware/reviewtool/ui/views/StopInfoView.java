package de.setsoftware.reviewtool.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.changestructure.CurrentStop;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.StopSelectionListener;
import de.setsoftware.reviewtool.ui.dialogs.ViewHelper;

/**
 * A view that contains detailed information on a tour stop fragment.
 */
public class StopInfoView extends ViewPart implements StopSelectionListener {

    private Composite comp;
    private Composite currentContent;

    @Override
    public void createPartControl(Composite comp) {
        this.comp = comp;

        CurrentStop.registerListener(this);
    }

    @Override
    public void setFocus() {
        if (this.currentContent != null && !this.currentContent.isDisposed()) {
            this.currentContent.setFocus();
        }
    }

    @Override
    public void notifyStopChange(Stop stop) {
        this.disposeOldContent();
        if (this.comp.isDisposed()) {
            CurrentStop.unregisterListener(this);
            return;
        }
        if (stop == null) {
            this.currentContent = this.createIdleContent("No review tour stop selected");
        } else {
            try {
                this.currentContent = this.createStopContent(stop);
            } catch (RuntimeException e) {
                this.currentContent = this.createIdleContent("Error while creating view " + e);                
            }
        }
        this.comp.layout();
    }

    private ScrolledComposite createStopContent(Stop stop) {
        final ScrolledComposite scroll = new ScrolledComposite(this.comp, SWT.VERTICAL | SWT.HORIZONTAL);
        scroll.setExpandHorizontal(true);
        scroll.setExpandVertical(true);
        scroll.setMinSize(300, 200);

        final Composite scrollContent = new Composite(scroll, SWT.NULL);

        final GridLayout layout = new GridLayout(2, true);
        scrollContent.setLayout(layout);

        ViewDataSource.get().getStopViewer().createStopView(this, scrollContent, stop);

        scroll.setContent(scrollContent);
        return scroll;
    }

    private Composite createIdleContent(String text) {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());
        final Label label = new Label(panel, SWT.NULL);
        label.setText(text);
        ViewHelper.createContextMenuWithoutSelectionProvider(this, label);
        return panel;
    }

    private void disposeOldContent() {
        if (this.currentContent != null) {
            this.currentContent.dispose();
        }
    }

}
