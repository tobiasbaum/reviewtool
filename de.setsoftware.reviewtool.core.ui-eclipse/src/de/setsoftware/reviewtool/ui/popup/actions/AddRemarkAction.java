package de.setsoftware.reviewtool.ui.popup.actions;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.MultiPageEditorPart;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.PathResource;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.model.api.Mode;
import de.setsoftware.reviewtool.model.api.PositionReference;
import de.setsoftware.reviewtool.model.remarks.RemarkType;
import de.setsoftware.reviewtool.model.remarks.ReviewRemark;
import de.setsoftware.reviewtool.model.remarks.ReviewRemarkException;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.CreateRemarkDialog;
import de.setsoftware.reviewtool.ui.dialogs.ViewHelper;
import de.setsoftware.reviewtool.ui.dialogs.CreateRemarkDialog.CreateDialogCallback;

/**
 * Action that adds a review remark (after prompting the user for details with a dialog).
 */
public class AddRemarkAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        PositionTransformer.initializeCacheInBackground();
        if (ReviewPlugin.getInstance().getMode() == Mode.IDLE) {
            ReviewPlugin.getInstance().startReview();
        }

        final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        final IEditorInput input = activeEditor != null ? activeEditor.getEditorInput() : null;

        ISelection sel = HandlerUtil.getActiveMenuSelection(event);
        if (sel == null && activeEditor != null) {
            sel = this.getTextSelection(activeEditor);
        }

        final Pair<? extends Object, Integer> selectionPos = ViewHelper.extractFileAndLineFromSelection(sel, input);
        if (selectionPos != null) {
            if (selectionPos.getFirst() instanceof IResource) {
                this.createMarker((IResource) selectionPos.getFirst(), selectionPos.getSecond());
            } else {
                this.createMarker((IPath) selectionPos.getFirst(), selectionPos.getSecond());
            }
        } else {
            if (sel != null) {
                Logger.debug("Unsupported selection, type=" + sel.getClass());
            }
            this.createMarker(ResourcesPlugin.getWorkspace().getRoot(), 0);
        }
        return null;
    }

    private void createMarker(final IResource resource, final int line) throws ExecutionException {
        try {
            final boolean isFile = resource.getType() == IResource.FILE;
            final Set<PositionReference> allowedRefs = this.determineAllowedRefs(line, isFile);
            final String prefillText = this.determinePrefillText();
            CreateRemarkDialog.get(allowedRefs, prefillText, new CreateDialogCallback() {
                @Override
                public void execute(String text, RemarkType type, PositionReference chosenRef) {
                    try {
                        final ReviewStateManager p = ReviewPlugin.getPersistence();
                        final IResource resourceFiltered = chosenRef != PositionReference.GLOBAL
                                ? resource : ResourcesPlugin.getWorkspace().getRoot();
                        final int lineFiltered = chosenRef == PositionReference.LINE ? line : 0;
                        final String reviewer = p.getReviewerForCurrentRound();
                        final ReviewRemark remark = ReviewRemark.create(
                                new EclipseResource(resourceFiltered),
                                reviewer,
                                text,
                                lineFiltered,
                                type);
                        p.saveRemark(remark);
                        AddRemarkAction.this.logRemarkCreated(resource.getFullPath(), type, lineFiltered);
                    } catch (final ReviewRemarkException e) {
                        throw new ReviewtoolException(e);
                    }
                }
            });
        } catch (final ReviewtoolException e) {
            throw new ExecutionException("error creating marker", e);
        }
    }

    private void createMarker(final IPath path, final int line) throws ExecutionException {
        try {
            final Set<PositionReference> allowedRefs = this.determineAllowedRefs(line, !path.toFile().isDirectory());
            final String prefillText = this.determinePrefillText();
            CreateRemarkDialog.get(allowedRefs, prefillText, new CreateDialogCallback() {
                @Override
                public void execute(String text, RemarkType type, PositionReference chosenRef) {
                    try {
                        final ReviewStateManager p = ReviewPlugin.getPersistence();
                        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
                        final IPath pathFiltered = chosenRef != PositionReference.GLOBAL
                                ? path : workspace.getRoot().getLocation();
                        final int lineFiltered = chosenRef == PositionReference.LINE ? line : 0;
                        final String reviewer = p.getReviewerForCurrentRound();
                        final ReviewRemark remark = ReviewRemark.create(
                                new PathResource(pathFiltered.makeAbsolute().toFile()),
                                reviewer,
                                text,
                                lineFiltered,
                                type);
                        p.saveRemark(remark);
                        AddRemarkAction.this.logRemarkCreated(path, type, lineFiltered);
                    } catch (final ReviewRemarkException e) {
                        throw new ReviewtoolException(e);
                    }
                }
            });
        } catch (final ReviewtoolException e) {
            throw new ExecutionException("error creating marker", e);
        }
    }

    private String determinePrefillText() {
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return "";
        }
        final IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return "";
        }
        final IEditorPart part = page.getActiveEditor();
        if (part == null) {
            return "";
        }
        final ITextSelection sel = this.getTextSelection(part);
        return sel == null ? "" : sel.getText();
    }

    private ITextSelection getTextSelection(final IEditorPart part) {
        final ISelection selection = part.getEditorSite().getSelectionProvider().getSelection();
        if (selection instanceof TextSelection) {
            return (ITextSelection) selection;
        } else {
            if (part instanceof MultiPageEditorPart) {
                final MultiPageEditorPart multiPage = (MultiPageEditorPart) part;
                for (final IEditorPart subPart : multiPage.findEditors(multiPage.getEditorInput())) {
                    final ITextSelection subSelection = this.getTextSelection(subPart);
                    if (subSelection != null) {
                        return subSelection;
                    }
                }
            }
            return null;
        }
    }

    private Set<PositionReference> determineAllowedRefs(final int line, boolean isFile) {
        if (!isFile) {
            return EnumSet.of(PositionReference.GLOBAL);
        } else if (line <= 0) {
            return EnumSet.of(PositionReference.GLOBAL, PositionReference.FILE);
        } else {
            return EnumSet.allOf(PositionReference.class);
        }
    }

    private void logRemarkCreated(final IPath path, RemarkType type, final int lineFiltered) {
        Telemetry.event("remarkCreated")
                .param("remarkType", type.name())
                .param("resource", path)
                .param("line", lineFiltered)
                .log();
    }
}
