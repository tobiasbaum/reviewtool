package de.setsoftware.reviewtool.ui.dialogs;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * A widget containing various infos for a commit as well as possibilities to (de)select it or assign it to a tour.
 */
public class CommitComposite extends Composite {

    private final Button checkbox;
    private final ICommit commit;
    private Set<? extends IClassification> chosenFilters;

    public CommitComposite(Composite parent, int style, ICommit commit, Set<? extends IClassification> chosenFilters) {
        super(parent, style);
        this.setLayout(new GridLayout(1, false));

        this.commit = commit;
        this.chosenFilters = chosenFilters;

        this.checkbox = new Button(this, SWT.CHECK);
        this.checkbox.setSelection(true);
        this.checkbox.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, true));
        this.updateHeadLabel();
        this.checkbox.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));

        final Text comment = new Text(this, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        comment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        comment.setText(commit.getMessage());

        this.setTabList(new Control[] {this.checkbox});
    }

    private void updateHeadLabel() {
        int stopCount = 0;
        int relevantStopCount = 0;
        final Set<IRevisionedFile> files = new HashSet<>();
        final Set<IRevisionedFile> relevantFiles = new HashSet<>();
        for (final IChange change : this.commit.getChanges()) {
            stopCount++;
            files.add(change.getTo());
            if (this.isRelevant(change)) {
                relevantStopCount++;
                relevantFiles.add(change.getTo());
            }
        }

        this.checkbox.setText(String.format("%s; %d file%s, %d stop%s (filtered: %d file%s, %d stop%s)",
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(this.commit.getTime()),
                files.size(),
                this.pluralS(files.size()),
                stopCount,
                this.pluralS(stopCount),
                relevantFiles.size(),
                this.pluralS(relevantFiles.size()),
                relevantStopCount,
                this.pluralS(relevantStopCount)));
    }

    private String pluralS(int size) {
        return size == 1 ? "" : "s";
    }

    private boolean isRelevant(IChange change) {
        return Collections.disjoint(this.chosenFilters, Arrays.asList(change.getClassification()));
    }

    public ICommit getCommit() {
        return this.commit;
    }

    public boolean isSelected() {
        return this.checkbox.getSelection();
    }

    public void updateFilters(Set<? extends IClassification> activeFilters) {
        this.chosenFilters = activeFilters;
        this.updateHeadLabel();
    }

}
