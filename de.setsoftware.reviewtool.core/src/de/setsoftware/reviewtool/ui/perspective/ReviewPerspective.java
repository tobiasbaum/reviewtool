package de.setsoftware.reviewtool.ui.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * A perspective to be used during review.
 */
public class ReviewPerspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        final String editorArea = layout.getEditorArea();

        //bottom left: review info and problems
        final IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.75f, editorArea);
        bottomLeft.addView("de.setsoftware.reviewtool.ui.views.reviewinfoview");
        bottomLeft.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottomLeft.addView(IPageLayout.ID_TASK_LIST);

        //bottom right: fragment info
        layout.addView("de.setsoftware.reviewtool.ui.views.fragmentinfoview", IPageLayout.RIGHT, 0.5f, "bottomLeft");

        //left: normal package explorer
        final IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.20f, editorArea);
        left.addView("org.eclipse.jdt.ui.PackageExplorer");
        left.addPlaceholder(IPageLayout.ID_BOOKMARKS);

        //right: review content
        layout.addView("de.setsoftware.reviewtool.ui.views.reviewcontentview", IPageLayout.RIGHT, 0.75f, editorArea);
    }

}
