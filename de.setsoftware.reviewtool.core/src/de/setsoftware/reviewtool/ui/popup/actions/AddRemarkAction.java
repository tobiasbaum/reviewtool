package de.setsoftware.reviewtool.ui.popup.actions;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.PositionTransformer;
import de.setsoftware.reviewtool.model.RemarkType;
import de.setsoftware.reviewtool.model.ReviewRemark;
import de.setsoftware.reviewtool.model.ReviewStateManager;
import de.setsoftware.reviewtool.plugin.ReviewPlugin;
import de.setsoftware.reviewtool.plugin.ReviewPlugin.Mode;
import de.setsoftware.reviewtool.telemetry.Telemetry;
import de.setsoftware.reviewtool.ui.dialogs.CreateRemarkDialog;
import de.setsoftware.reviewtool.ui.dialogs.CreateRemarkDialog.CreateDialogCallback;
import de.setsoftware.reviewtool.ui.dialogs.CreateRemarkDialog.PositionReference;
import de.setsoftware.reviewtool.ui.views.ViewHelper;

/**
 * Action that adds a review remark (after prompting the user for details with a dialog).
 */
public class AddRemarkAction extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        PositionTransformer.initializeCacheInBackground();
        if (ReviewPlugin.getInstance().getMode() == Mode.IDLE) {
            try {
                ReviewPlugin.getInstance().startReview();
            } catch (final CoreException e) {
                throw new ExecutionException("exception while starting review", e);
            }
        }

        final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        final IEditorInput input = activeEditor != null ? activeEditor.getEditorInput() : null;

        ISelection sel = HandlerUtil.getActiveMenuSelection(event);
        if (sel == null && activeEditor != null && activeEditor.getEditorSite().getSelectionProvider() != null) {
            sel = activeEditor.getEditorSite().getSelectionProvider().getSelection();
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
            CreateRemarkDialog.get(allowedRefs, new CreateDialogCallback() {
                @Override
                public void execute(String text, RemarkType type, PositionReference chosenRef) {
                    try {
                        final ReviewStateManager p = ReviewPlugin.getPersistence();
                        final IResource resourceFiltered = chosenRef != PositionReference.GLOBAL
                                ? resource : ResourcesPlugin.getWorkspace().getRoot();
                        final int lineFiltered = chosenRef == PositionReference.LINE ? line : 0;
                        final String reviewer = p.getReviewerForCurrentRound();
                        ReviewRemark.create(
                                p,
                                resourceFiltered,
                                reviewer,
                                text,
                                lineFiltered,
                                type).save();
                        Telemetry.get().remarkCreated(
                                type.name(),
                                resource.getFullPath().toString(),
                                lineFiltered);
                    } catch (final CoreException e) {
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
            CreateRemarkDialog.get(allowedRefs, new CreateDialogCallback() {
                @Override
                public void execute(String text, RemarkType type, PositionReference chosenRef) {
                    try {
                        final ReviewStateManager p = ReviewPlugin.getPersistence();
                        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
                        final IPath pathFiltered = chosenRef != PositionReference.GLOBAL
                                ? path : workspace.getRoot().getFullPath();
                        final int lineFiltered = chosenRef == PositionReference.LINE ? line : 0;
                        final String reviewer = p.getReviewerForCurrentRound();
                        ReviewRemark.createWithoutMarker(
                                p,
                                pathFiltered,
                                reviewer,
                                text,
                                lineFiltered,
                                type,
                                workspace).save();
                        Telemetry.get().remarkCreated(
                                type.name(),
                                path.toString(),
                                lineFiltered);
                    } catch (final CoreException e) {
                        throw new ReviewtoolException(e);
                    }
                }
            });
        } catch (final ReviewtoolException e) {
            throw new ExecutionException("error creating marker", e);
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

}
