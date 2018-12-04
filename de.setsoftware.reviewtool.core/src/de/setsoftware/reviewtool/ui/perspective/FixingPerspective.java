package de.setsoftware.reviewtool.ui.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * A perspective to be used during fixing.
 */
public class FixingPerspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        final String editorArea = layout.getEditorArea();

        //bottom left: review info and problems
        final IFolderLayout bottom = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.75f, editorArea);
        bottom.addView("de.setsoftware.reviewtool.ui.views.reviewinfoview");
        bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottom.addView(IPageLayout.ID_TASK_LIST);

        //left: normal package explorer
        final IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.20f, editorArea);
        left.addView("org.eclipse.jdt.ui.PackageExplorer");
        left.addPlaceholder(IPageLayout.ID_BOOKMARKS);

        //right: review remarks / fixing content
        //TODO
        //layout.addView("de.setsoftware.reviewtool.ui.views.reviewcontentview", IPageLayout.RIGHT, 0.75f, editorArea);
        //layout.addShowInPart("de.setsoftware.reviewtool.ui.views.reviewcontentview");

        //the standard Java actions shall be available
        layout.addActionSet("org.eclipse.jdt.ui.JavaActionSet");
    }

}
