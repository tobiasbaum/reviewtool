package de.setsoftware.reviewtool.ui.dialogs;

import java.net.URI;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Helper methods for views.
 */
public class ViewHelper {

    /**
     * Creates a context menu for the given control in the given view part and with the given selection provider.
     */
    public static void createContextMenu(final IViewPart viewPart, final Control control,
            final ISelectionProvider sel) {
        final MenuManager menuManager = new MenuManager();
        final Menu menu = menuManager.createContextMenu(control);
        control.setMenu(menu);
        viewPart.getSite().registerContextMenu(menuManager, sel);
        viewPart.getSite().setSelectionProvider(sel);
    }

    /**
     * Creates a context menu for a control for which no selection provider is available.
     */
    public static void createContextMenuWithoutSelectionProvider(final IViewPart viewPart, final Control control) {
        createContextMenu(viewPart, control, new ISelectionProvider() {
            private ISelection selection;

            @Override
            public void setSelection(final ISelection selection) {
                this.selection = selection;
            }

            @Override
            public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
            }

            @Override
            public ISelection getSelection() {
                return this.selection;
            }

            @Override
            public void addSelectionChangedListener(final ISelectionChangedListener listener) {
            }
        });
    }

    /**
     * Extracts the selected resource and line in that resource from the given selection and input. If the position
     * cannot be determined, the number zero is used. If the selection cannot be interpreted at all, null is returned.
     * The first part of the pair is either an {@link IResource} (preferred) or an {@link IPath}.
     */
    public static Pair<? extends Object, Integer> extractFileAndLineFromSelection(final ISelection sel,
            final Object input) throws ExecutionException {

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

    private static <T> T getAs(final Object o, final Class<T> class1) {
        if (class1.isInstance(o)) {
            return class1.cast(o);
        }
        if (o instanceof IAdaptable) {
            return class1.cast(((IAdaptable) o).getAdapter(class1));
        }
        return null;
    }

    private static Pair<? extends Object, Integer> handleTextSelection(final ITextSelection sel, final Object input) {

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

    private static Pair<? extends Object, Integer> handleStructuredSelection(final IStructuredSelection selection)
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
                return Pair.create(new Path(stop.getMostRecentFile().toLocalPath(stop.getWorkingCopy()).getPath()),
                        stop.isDetailedFragmentKnown() ? stop.getMostRecentFragment().getFrom().getLine() : 0);
            }
        }
        return null;
    }

    private static Pair<IResource, Integer> toPos(final IJavaElement type, final int line) throws ExecutionException {
        try {
            return Pair.create(type.getUnderlyingResource(), line);
        } catch (final JavaModelException e) {
            throw new ExecutionException("Error while adding marker", e);
        }
    }

    /**
     * Sets the selection in an editor.
     */
    public static void setSelection(IEditorPart part, TextSelection textSelection) {
        if (part instanceof MultiPageEditorPart) {
            final MultiPageEditorPart multiPage = (MultiPageEditorPart) part;
            for (final IEditorPart subPart : multiPage.findEditors(multiPage.getEditorInput())) {
                setSelection(subPart, textSelection);
            }
        } else {
            final IEditorSite editorSite = part.getEditorSite();
            final ISelectionProvider sp = editorSite == null ? null : editorSite.getSelectionProvider();
            if (sp == null) {
                Logger.debug("cannot select, selection provider is null");
                return;
            }
            sp.setSelection(textSelection);
        }
    }

    /**
     * Opens an editor for the given marker.
     * Ensures that a decent editor is used.
     */
    public static void openEditorForMarker(IWorkbenchPage page, IMarker marker, boolean forceTextEditor)
        throws CoreException {

        final String editorId = findEditor(forceTextEditor, marker.getResource().getName());
        marker.setAttribute(IDE.EDITOR_ID_ATTR, editorId);
        IDE.openEditor(page, marker);
    }

    /**
     * Opens an editor for the given file. Returns the opened editor.
     * Ensures that a decent editor is used.
     */
    public static IEditorPart openEditorForFile(IWorkbenchPage page, IFileStore fileStore, boolean forceTextEditor)
        throws PartInitException {

        final String editorId = findEditor(forceTextEditor, fileStore.getName());
        return page.openEditor(new FileStoreEditorInput(fileStore), editorId);
    }

    private static String findEditor(boolean forceTextEditor, String filename) {
        if (forceTextEditor) {
            return getTextEditorId();
        }

        final IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();
        final IEditorDescriptor defaultEditor = editorReg.getDefaultEditor(filename);
        if (defaultEditor != null && !isUnsuitable(defaultEditor)) {
            return defaultEditor.getId();
        }
        for (final IEditorDescriptor editor : editorReg.getEditors(filename)) {
            if (!isUnsuitable(editor)) {
                return editor.getId();
            }
        }
        return getTextEditorId();
    }

    private static boolean isUnsuitable(IEditorDescriptor editor) {
        return editor.isOpenExternal()
            || editor.getId().equals("org.eclipse.ui.browser.editorSupport");
    }

    private static String getTextEditorId() {
        return "org.eclipse.ui.DefaultTextEditor";
    }

}
