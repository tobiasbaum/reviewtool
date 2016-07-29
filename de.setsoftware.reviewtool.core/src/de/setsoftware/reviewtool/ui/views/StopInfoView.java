package de.setsoftware.reviewtool.ui.views;

import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;

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
    }

    @Override
    public void notifyStopChange(Stop fragment) {
        this.disposeOldContent();
        if (fragment == null) {
            this.currentContent = this.createIdleContent("No review tour stop selected");
        } else {
            this.currentContent = this.createFragmentContent(fragment);
        }
        this.comp.layout();
    }

    private ScrolledComposite createFragmentContent(Stop fragment) {
        final ScrolledComposite scroll = new ScrolledComposite(this.comp, SWT.VERTICAL | SWT.HORIZONTAL);
        scroll.setExpandHorizontal(true);
        scroll.setExpandVertical(true);
        scroll.setMinSize(300, 200);

        final Composite scrollContent = new Composite(scroll, SWT.NULL);

        final FillLayout layout = new FillLayout();
        scrollContent.setLayout(layout);

        final Iterator<FileInRevision> it = fragment.getHistory().iterator();
        FileInRevision oldRevision = null;
        while (it.hasNext()) {
            final FileInRevision newRevision = it.next();
            if (oldRevision != null) {
                this.createContentLabel(scrollContent, fragment, oldRevision, newRevision);
                oldRevision = null;
            } else {
                oldRevision = newRevision;
            }
        }

        scroll.setContent(scrollContent);
        return scroll;
    }

    /**
     * Builds a string containing the concatenated contents of the fragments passed.
     * @param fragments The list of fragments.
     * @return The concatenated contents of the fragments passed.
     */
    private String mapFragmentsToString(final List<Fragment> fragments) {
        final StringBuilder text = new StringBuilder();
        boolean first = true;
        for (final Fragment f : fragments) {
            if (first) {
                first = false;
            } else {
                text.append("\n...\n\n");
            }
            text.append(f.getContent());
        }
        return text.toString();
    }

    private void createContentLabel(final Composite scrollContent, final Stop fragment,
            final FileInRevision oldRevision, final FileInRevision newRevision) {
        final List<Fragment> oldContent = fragment.getContentFor(oldRevision);
        final List<Fragment> newContent = fragment.getContentFor(newRevision);
        if (oldContent == null || newContent == null) {
            final Label label = new Label(scrollContent, SWT.NULL);
            label.setText("binary");
            label.setFont(
                    JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
            ViewHelper.createContextMenuWithoutSelectionProvider(this, label);
        } else {
            final CompareConfiguration compareConfiguration = new CompareConfiguration();
            compareConfiguration.setLeftLabel(oldRevision.toString());
            compareConfiguration.setRightLabel(newRevision.toString());
            final TextMergeViewer viewer = new TextMergeViewer(scrollContent, SWT.BORDER, compareConfiguration);
            viewer.setInput(new DiffNode(new TextItem(oldRevision.toString(), this.mapFragmentsToString(oldContent),
                    System.currentTimeMillis()),
                    new TextItem(newRevision.toString(), this.mapFragmentsToString(newContent),
                            System.currentTimeMillis())));
        }
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
