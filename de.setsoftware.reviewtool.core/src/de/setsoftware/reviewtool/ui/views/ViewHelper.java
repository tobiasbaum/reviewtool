package de.setsoftware.reviewtool.ui.views;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.ViewPart;

/**
 * Helper methods for views.
 */
public class ViewHelper {

    /**
     * Creates a context menu for the given control in the given view part and with the given selection
     * provider.
     */
    public static void createContextMenu(ViewPart viewPart, Control control, ISelectionProvider sel) {
        final MenuManager menuManager = new MenuManager();
        final Menu menu = menuManager.createContextMenu(control);
        control.setMenu(menu);
        viewPart.getSite().registerContextMenu(menuManager, sel);
        viewPart.getSite().setSelectionProvider(sel);
    }

    /**
     * Creates a context menu for a control for which no selection provider is available.
     */
    public static void createContextMenuWithoutSelectionProvider(ViewPart viewPart, Control control) {
        createContextMenu(viewPart, control, new ISelectionProvider() {
            private ISelection selection;

            @Override
            public void setSelection(ISelection selection) {
                this.selection = selection;
            }

            @Override
            public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            }

            @Override
            public ISelection getSelection() {
                return this.selection;
            }

            @Override
            public void addSelectionChangedListener(ISelectionChangedListener listener) {
            }
        });
    }

}
