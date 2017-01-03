package de.setsoftware.reviewtool.ui.views;

import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.Stop;

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

    /**
     * Extracts the selected resource and line in that resource from the given selection and input.
     * If the position cannot be determined, the number zero is used. If the selection cannot be
     * interpreted at all, null is returned. The first part of the pair is either an {@link IResource} (preferred)
     * or an {@link IPath}.
     */
    public static Pair<? extends Object, Integer> extractFileAndLineFromSelection(ISelection sel, Object input)
        throws ExecutionException {

        final ITextSelection textSel = getAs(sel, ITextSelection.class);
        if (textSel != null) {
            return handleTextSelection(textSel, input);
        }
        final IStructuredSelection structuredSel = getAs(sel, IStructuredSelection.class);
        if (structuredSel != null) {
            return handleStructuredSelection((IStructuredSelection) sel);
        }
        return null;
    }

    private static<T> T getAs(Object o, Class<T> class1) {
        if (class1.isInstance(o)) {
            return class1.cast(o);
        }
        if (o instanceof IAdaptable) {
            return class1.cast(((IAdaptable) o).getAdapter(class1));
        }
        return null;
    }

    private static Pair<? extends Object, Integer> handleTextSelection(ITextSelection sel, Object input)
            throws ExecutionException {

        if (input instanceof FileEditorInput) {
            final IFile f = ((FileEditorInput) input).getFile();
            return Pair.create(f, sel.getStartLine() + 1);
        } else if (input instanceof FileStoreEditorInput) {
            final URI uri = ((FileStoreEditorInput) input).getURI();
            final IPath path = Path.fromOSString(uri.getPath());
            return Pair.create(path, sel.getStartLine() + 1);
        } else {
            return null;
        }
    }

    private static Pair<? extends Object, Integer> handleStructuredSelection(IStructuredSelection selection)
            throws ExecutionException {

        for (final Object element : selection.toArray()) {
            final IJavaElement je = getAs(element, IJavaElement.class);
            if (je != null) {
                return toPos(je, 0);
            }
            final IResource res = getAs(element, IResource.class);
            if (res != null) {
                return Pair.create(res, 0);
            }
            final Stop stop = getAs(element, Stop.class);
            if (stop != null) {
                return Pair.create(
                        stop.getMostRecentFile().toLocalPath(),
                        stop.isDetailedFragmentKnown() ? stop.getMostRecentFragment().getFrom().getLine() : 0);
            }
        }
        return null;
    }

    private static Pair<IResource, Integer> toPos(IJavaElement type, int line) throws ExecutionException {
        try {
            return Pair.create(type.getUnderlyingResource(), line);
        } catch (final JavaModelException e) {
            throw new ExecutionException("Error while adding marker", e);
        }
    }

}
