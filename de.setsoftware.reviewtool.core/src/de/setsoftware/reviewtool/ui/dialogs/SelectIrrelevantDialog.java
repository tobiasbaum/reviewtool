package de.setsoftware.reviewtool.ui.dialogs;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.setsoftware.reviewtool.base.Multiset;
import de.setsoftware.reviewtool.model.api.IClassification;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.ReviewRoundInfo;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview.UserSelectedReductions;

/**
 * A dialog that let's the user choose which of several filters that mark changes as irrelevant to apply.
 */
public class SelectIrrelevantDialog extends Dialog {

    private final List<? extends ICommit> allCommits;
    private final Multiset<? extends IClassification> filterChoices;
    private final List<ReviewRoundInfo> reviewRounds;
    private List<CommitComposite> commitComposites;
    private List<Button> filterCheckboxes;
    private List<ICommit> chosenCommitSubset;
    private Set<IClassification> chosenFilterSubset;

    protected SelectIrrelevantDialog(
            Shell parentShell,
            List<? extends ICommit> changes,
            Multiset<? extends IClassification> filterChoices,
            List<ReviewRoundInfo> reviewRounds) {
        super(parentShell);
        this.setShellStyle(this.getShellStyle() | SWT.RESIZE);
        this.allCommits = changes;
        this.filterChoices = filterChoices;
        this.reviewRounds = reviewRounds;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Select commits and choose filters to apply");
        DialogHelper.restoreSavedSize(newShell, this, 500, 400);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        final Composite comp = (Composite) super.createDialogArea(parent);

        final GridLayout layout = (GridLayout) comp.getLayout();
        layout.numColumns = 1;

        final Group commitGroup = new Group(comp, SWT.NONE);
        commitGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        commitGroup.setText("Commits");
        commitGroup.setLayout(new FillLayout());

        final ScrolledComposite scroll = new ScrolledComposite(commitGroup, SWT.H_SCROLL | SWT.V_SCROLL);

        final Composite scrollContent = new Composite(scroll, SWT.NONE);
        scrollContent.setLayout(new GridLayout(1, false));

        final PriorityQueue<ReviewRoundInfo> reviewRoundQueue = new PriorityQueue<>(this.reviewRounds);
        this.commitComposites = new ArrayList<>();
        for (final ICommit commit : this.allCommits) {
            while (!reviewRoundQueue.isEmpty() && reviewRoundQueue.peek().getTime().compareTo(commit.getTime()) < 0) {
                this.createReviewRoundWidget(reviewRoundQueue.remove(), scrollContent);
            }

            final CommitComposite cc = new CommitComposite(scrollContent, SWT.NONE, commit, this.filterChoices.keySet());
            this.commitComposites.add(cc);
            cc.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        }
        while (!reviewRoundQueue.isEmpty()) {
            this.createReviewRoundWidget(reviewRoundQueue.remove(), scrollContent);
        }

        scroll.setContent(scrollContent);
        scroll.setExpandHorizontal(true);
        scroll.setExpandVertical(true);
        scroll.setMinWidth(300);
        scroll.setMinHeight(scrollContent.computeSize(scroll.getMinWidth(), SWT.DEFAULT).y);


        final Group buttonGroup = new Group(comp, SWT.NONE);
        buttonGroup.setText("Filters to apply");
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        buttonGroup.setLayout(gridLayout);
        buttonGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.filterCheckboxes = new ArrayList<>();
        final SelectionListener filterCheckboxListener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                SelectIrrelevantDialog.this.updateCountsInCommits();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                SelectIrrelevantDialog.this.updateCountsInCommits();
            }
        };
        for (final IClassification choice : this.filterChoices.keySet()) {
            final Button b = new Button(buttonGroup, SWT.CHECK);
            b.setText(this.createText(choice));
            b.setData(choice);
            b.addSelectionListener(filterCheckboxListener);
            this.filterCheckboxes.add(b);
        }

        this.restoreSavedSelection();

        return comp;
    }

    private void createReviewRoundWidget(ReviewRoundInfo round, Composite scrollContent) {
        final Label label = new Label(scrollContent, SWT.NONE);
        label.setText(String.format("----------------- End of Review %d: %s, %s -----------------",
                round.getNumber(),
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(round.getTime()),
                round.getReviewer()));
        label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
    }

    private void updateCountsInCommits() {
        final Set<IClassification> activeFilters = new LinkedHashSet<>();
        for (final Button b : this.filterCheckboxes) {
            final IClassification data = (IClassification) b.getData();
            if (b.getSelection()) {
                activeFilters.add(data);
            }
        }

        for (final CommitComposite cc : this.commitComposites) {
            cc.updateFilters(activeFilters);
        }
    }

    private String createText(IClassification choice) {
        return String.format("%s (applies to %d changes)",
                choice.getName(),
                this.filterChoices.get(choice));
    }

    private void restoreSavedSelection() {
        for (final Button b : this.filterCheckboxes) {
            final IClassification data = (IClassification) b.getData();
            b.setSelection(this.getSavedSelectionState(data));
        }
    }

    @Override
    protected void okPressed() {
        this.chosenCommitSubset = new ArrayList<>();
        for (final CommitComposite c : this.commitComposites) {
            if (c.isSelected()) {
                this.chosenCommitSubset.add(c.getCommit());
            }
        }

        this.chosenFilterSubset = new LinkedHashSet<>();
        for (final Button b : this.filterCheckboxes) {
            final IClassification data = (IClassification) b.getData();
            if (b.getSelection()) {
                this.chosenFilterSubset.add(data);
            }
            this.saveSelectionState(data, b.getSelection());
        }

        DialogHelper.saveDialogSize(this);
        super.okPressed();
    }

    private boolean getSavedSelectionState(IClassification data) {
        final String s = DialogHelper.getSetting(this.makeSettingId(data));
        if (s.isEmpty()) {
            //default: activate filter
            return true;
        }
        return Boolean.parseBoolean(s);
    }

    private void saveSelectionState(IClassification data, boolean selection) {
        DialogHelper.saveSetting(this.makeSettingId(data), Boolean.toString(selection));
    }

    private String makeSettingId(IClassification data) {
        return data.getName().replaceAll("[^a-zA-Z0-9]", "");
    }

    @Override
    protected void cancelPressed() {
        DialogHelper.saveDialogSize(this);
        super.cancelPressed();
    }

    /**
     * Lets the user select the filters he wants to apply.
     * When the user cancels, null is returned.
     */
    public static UserSelectedReductions show(
            List<? extends ICommit> changes,
            Multiset<IClassification> filterChoices,
            List<ReviewRoundInfo> reviewRounds) {
        final Shell s = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        final SelectIrrelevantDialog dialog =
                new SelectIrrelevantDialog(s, changes, filterChoices, reviewRounds);
        final int ret = dialog.open();
        if (ret != OK) {
            return null;
        }
        if (dialog.chosenFilterSubset == null) {
            return null;
        }
        return new UserSelectedReductions(
                dialog.chosenCommitSubset,
                dialog.chosenFilterSubset);
    }

}
