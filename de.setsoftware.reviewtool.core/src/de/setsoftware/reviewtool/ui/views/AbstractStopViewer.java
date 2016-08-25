package de.setsoftware.reviewtool.ui.views;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.ui.IStopViewer;

/**
 * Represents the foundation for concrete stop viewers.
 */
public abstract class AbstractStopViewer implements IStopViewer {

    /**
     * Builds a string containing the concatenated contents of the fragments passed.
     * @param fragments The list of fragments.
     * @return The concatenated contents of the fragments passed.
     */
    protected String mapFragmentsToString(final List<Fragment> fragments) {
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

    /**
     * Creates a difference viewer.
     * @param view The {@link ViewPart} to use.
     * @param parent The {@link Composite} to use as parent for the difference viewer.
     * @param sourceRevision The source revision.
     * @param targetRevision The target revision.
     * @param sourceFragments The source fragments.
     * @param targetFragments The target fragments.
     */
    protected void createDiffViewer(final ViewPart view, final Composite parent,
            final FileInRevision sourceRevision, final FileInRevision targetRevision,
            final List<Fragment> sourceFragments, final List<Fragment> targetFragments) {
        if (sourceFragments.isEmpty() || targetFragments.isEmpty()) {
            this.createBinaryHunkViewer(view, parent);
        } else {
            this.createTextHunkViewer(parent, sourceRevision, targetRevision, sourceFragments, targetFragments);
        }
    }

    private void createBinaryHunkViewer(final ViewPart view, final Composite parent) {
        final Label label = new Label(parent, SWT.NULL);
        label.setText("binary");
        label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        ViewHelper.createContextMenuWithoutSelectionProvider(view, label);
    }

    private void createTextHunkViewer(final Composite parent,
            final FileInRevision sourceRevision, final FileInRevision targetRevision,
            final List<Fragment> sourceFragments, final List<Fragment> targetFragments) {
        final CompareConfiguration compareConfiguration = new CompareConfiguration();
        compareConfiguration.setLeftLabel(sourceRevision.getRevision().toString());
        compareConfiguration.setRightLabel(targetRevision.getRevision().toString());
        final TextMergeViewer viewer = new TextMergeViewer(parent, SWT.BORDER, compareConfiguration);
        viewer.setInput(new DiffNode(
                new TextItem(sourceRevision.getRevision().toString(),
                        this.mapFragmentsToString(sourceFragments),
                        System.currentTimeMillis()),
                new TextItem(targetRevision.getRevision().toString(),
                        this.mapFragmentsToString(targetFragments),
                        System.currentTimeMillis())));
    }

}
