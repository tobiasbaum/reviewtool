package de.setsoftware.reviewtool.plugin;

import java.util.Arrays;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

/**
 * Editor for {@link RelationMatcherPreferences}.
 */
public class RelationMatcherPreferenceEditor extends Composite {

    private final List inactiveSide;
    private final List activeSide;
    private final RelationMatcherPreferences relations;

    public RelationMatcherPreferenceEditor(Composite parent, int style, RelationMatcherPreferences relations) {
        super(parent, style);
        this.relations = relations;
        this.setLayout(new FillLayout());

        final Group g = new Group(this, SWT.SHADOW_NONE);
        g.setText("Relation types for ordering and grouping");

        final GridLayout mainLayout = new GridLayout(3, false);
        g.setLayout(mainLayout);

        final Label inactiveLabel = new Label(g, SWT.NONE);
        inactiveLabel.setText("Inactive relation types");
        inactiveLabel.setLayoutData(growSpanGridData(2, 1, false, false));

        final Label activeLabel = new Label(g, SWT.NONE);
        activeLabel.setText("Active relation types");

        this.inactiveSide = new List(g, SWT.BORDER | SWT.MULTI);
        this.inactiveSide.setLayoutData(growSpanGridData(1, 2, true, true));

        final Composite middleButtonBar = new Composite(g, SWT.NONE);
        middleButtonBar.setLayout(new FillLayout(SWT.VERTICAL));
        middleButtonBar.setLayoutData(growSpanGridData(1, 2, false, false));

        final Button activate = new Button(middleButtonBar, SWT.PUSH);
        activate.setText(">>");
        activate.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.activateSelectedItems();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.activateSelectedItems();
            }
        });

        final Button deactivate = new Button(middleButtonBar, SWT.PUSH);
        deactivate.setText("<<");
        deactivate.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.deactivateSelectedItems();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.deactivateSelectedItems();
            }
        });

        this.activeSide = new List(g, SWT.BORDER | SWT.MULTI);
        this.activeSide.setLayoutData(growSpanGridData(1, 1, true, true));

        final Composite activeButtonBar = new Composite(g, SWT.NONE);
        activeButtonBar.setLayout(new FillLayout(SWT.HORIZONTAL));

        final Button moveUp = new Button(activeButtonBar, SWT.PUSH);
        moveUp.setText("Increase priority");
        moveUp.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.moveUp();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.moveUp();
            }
        });

        final Button moveDown = new Button(activeButtonBar, SWT.PUSH);
        moveDown.setText("Decrease priority");
        moveDown.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.moveDown();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.moveDown();
            }
        });

        final Button toggleExplicitness = new Button(activeButtonBar, SWT.PUSH);
        toggleExplicitness.setText("Toggle explicitness");
        toggleExplicitness.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.toggleExplicitness();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                RelationMatcherPreferenceEditor.this.toggleExplicitness();
            }
        });

        this.refillLists();
    }

    private static GridData growSpanGridData(
            int horizontalSpan, int verticalSpan, boolean horizontalGrow, boolean verticalGrow) {
        final GridData ret = new GridData();
        ret.horizontalSpan = horizontalSpan;
        ret.verticalSpan = verticalSpan;
        ret.grabExcessHorizontalSpace = horizontalGrow;
        ret.grabExcessHorizontalSpace = verticalGrow;
        if (horizontalGrow) {
            ret.horizontalAlignment = GridData.FILL;
            ret.minimumWidth = 100;
        }
        if (verticalGrow) {
            ret.verticalAlignment = GridData.FILL;
            ret.minimumHeight = 100;
        }
        return ret;
    }

    private void refillLists() {
        this.inactiveSide.setItems(this.relations.getInactiveForUser().toArray(new String[0]));
        this.activeSide.setItems(this.relations.getActiveForUser().toArray(new String[0]));
    }

    private void activateSelectedItems() {
        for (final String s : this.inactiveSide.getSelection()) {
            this.relations.activate(s);
        }
        this.refillLists();
    }

    private void deactivateSelectedItems() {
        for (final String s : this.activeSide.getSelection()) {
            this.relations.deactivate(s);
        }
        this.refillLists();
    }

    protected void moveDown() {
        final String[] selection = this.activeSide.getSelection();
        this.relations.moveDown(Arrays.asList(selection));
        this.refillLists();
        this.activeSide.setSelection(selection);
    }

    protected void moveUp() {
        final String[] selection = this.activeSide.getSelection();
        this.relations.moveUp(Arrays.asList(selection));
        this.refillLists();
        this.activeSide.setSelection(selection);
    }

    protected void toggleExplicitness() {
        final int[] selection = this.activeSide.getSelectionIndices();
        for (final String s : this.activeSide.getSelection()) {
            this.relations.toggleExplicitness(s);
        }
        this.refillLists();
        this.activeSide.setSelection(selection);
    }

    public void save(IPreferenceStore preferenceStore) {
        this.relations.save(preferenceStore);
    }

    public void performDefaults() {
        this.relations.resetToDefaults();
        this.refillLists();
    }

}
