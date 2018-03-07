package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.hyperlink.DefaultHyperlinkPresenter;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.ViewPart;

import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.changestructure.CommitsInReview.CommitsInReviewListener;
import de.setsoftware.reviewtool.ui.api.ReviewUi;

/**
 * Basic text summary presentation with hyper-links to expand and shrink some
 * summary parts or for trigger actions.
 */
public class ReviewContentSummaryView extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "de.setsoftware.reviewtool.summary.ReviewContentSummaryView";
    private TextViewer viewer;
    private CommitsInReviewListener commitsListener;
    private Controller controller = new Controller(this);
    private List<? extends IRegion> links = new ArrayList<>();

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TextViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setDocument(new Document());
        viewer.setEditable(false);

        IHyperlinkDetector linkDetector = new IHyperlinkDetector() {
            @Override
            public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
                    boolean canShowMultipleHyperlinks) {
                for (IRegion link : links) {
                    if (region.getOffset() >= link.getOffset()
                            && region.getOffset() <= link.getOffset() + link.getLength()) {
                        IHyperlink hyperlink = new IHyperlink() {

                            @Override
                            public IRegion getHyperlinkRegion() {
                                return link;
                            }

                            @Override
                            public String getTypeLabel() {
                                return null;
                            }

                            @Override
                            public String getHyperlinkText() {
                                return null;
                            }

                            @Override
                            public void open() {
                                controller.onClick(link);
                            }
                        };
                        return new IHyperlink[] { hyperlink };
                    }
                }
                return null;
            }
        };
        viewer.setHyperlinkDetectors(new IHyperlinkDetector[] { linkDetector }, 0);

        Display display = Display.getCurrent();
        Color color = display.getSystemColor(SWT.COLOR_BLUE);
        viewer.setHyperlinkPresenter(new DefaultHyperlinkPresenter(color));

        hookContextMenu();

        commitsListener = new CommitsInReviewListener() {

            // SWTException: Invalid thread access, if not using asyncExec.
            @Override
            public void notifyCommits(List<ICommit> currentCommits) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        controller.processCommits(ReviewUi.getCommitsInReview());
                    }
                });
            }
        };
        ReviewUi.registerCommitsInReviewListener(commitsListener);
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    /**
     * Update text to show.
     */
    public void setText(String text) {
        int i = viewer.getTopIndex();
        viewer.getDocument().set(text);
        viewer.setTopIndex(i);
    }

    /**
     * Set and mark up hyperlinks regions.
     */
    public void setHyperLinks(List<? extends IRegion> links) {
        this.links = links;

        StyledText text = viewer.getTextWidget();
        if (text == null || text.isDisposed()) {
            return;
        }

        Display display = Display.getCurrent();
        Color color = display.getSystemColor(SWT.COLOR_BLUE);

        for (IRegion link : links) {
            StyleRange styleRange = new StyleRange(link.getOffset(), link.getLength(), color, null);
            styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            styleRange.underline = true;
            text.setStyleRange(styleRange);
        }
    }
}
