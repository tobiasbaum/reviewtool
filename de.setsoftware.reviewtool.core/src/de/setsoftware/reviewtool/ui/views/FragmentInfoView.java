package de.setsoftware.reviewtool.ui.views;

import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.changestructure.FileFragment;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.SliceFragment;

/**
 * A view that contains detailed information on a slice fragment.
 */
public class FragmentInfoView extends ViewPart implements FragmentSelectionListener {

    private Composite comp;
    private Composite currentContent;

    @Override
    public void createPartControl(Composite comp) {
        this.comp = comp;

        CurrentFragment.registerListener(this);
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void notifyFragmentChange(SliceFragment fragment) {
        this.disposeOldContent();
        if (fragment == null) {
            this.currentContent = this.createIdleContent("Kein Fragment gewählt");
        } else {
            this.currentContent = this.createFragmentContent(fragment);
        }
        this.comp.layout();
    }

    private ScrolledComposite createFragmentContent(SliceFragment fragment) {
        final ScrolledComposite scroll = new ScrolledComposite(this.comp, SWT.VERTICAL | SWT.HORIZONTAL);
        scroll.setExpandHorizontal(true);
        scroll.setExpandVertical(true);
        scroll.setMinSize(300, 200);

        final Composite scrollContent = new Composite(scroll, SWT.NULL);

        final GridLayout layout = new GridLayout();
        layout.numColumns = fragment.getHistory().size();
        scrollContent.setLayout(layout);

        for (final FileInRevision revision : fragment.getHistory()) {
            final Label revLabel = new Label(scrollContent, SWT.NULL);
            revLabel.setText("Rev. " + revision.getRevision().toString());
            revLabel.setToolTipText(revision.getPath());
        }

        for (final FileInRevision revision : fragment.getHistory()) {
            this.createContentLabel(scrollContent, fragment.getContentFor(revision));
        }

        scroll.setContent(scrollContent);
        return scroll;
    }

    private void createContentLabel(Composite scrollContent, List<FileFragment> content) {
        if (content == null) {
            final Label label = new Label(scrollContent, SWT.NULL);
            label.setText("binär");
            label.setFont(
                    JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        } else {
            final Label label = new Label(scrollContent, SWT.BORDER);
            final StringBuilder text = new StringBuilder();
            boolean first = true;
            for (final FileFragment f : content) {
                if (first) {
                    first = false;
                } else {
                    text.append("\n...\n\n");
                }
                text.append(f.getContent());
            }
            label.setText(text.toString());
            label.setFont(
                    JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
            label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        }
    }

    private Composite createIdleContent(String text) {
        final Composite panel = new Composite(this.comp, SWT.NULL);
        panel.setLayout(new FillLayout());
        final Label label = new Label(panel, SWT.NULL);
        label.setText(text);
        return panel;
    }

    private void disposeOldContent() {
        if (this.currentContent != null) {
            this.currentContent.dispose();
        }
    }

}
