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
    private final Controller controller = new Controller(this);
    private List<SummaryTextPart> links = new ArrayList<>();

    @Override
    public void createPartControl(Composite parent) {
        this.viewer = new TextViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        this.viewer.setDocument(new Document());
        this.viewer.setEditable(false);

        final IHyperlinkDetector linkDetector = new IHyperlinkDetector() {
            @Override
            public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
                    boolean canShowMultipleHyperlinks) {
                for (final IRegion link : ReviewContentSummaryView.this.links) {
                    if (region.getOffset() >= link.getOffset()
                            && region.getOffset() <= link.getOffset() + link.getLength()) {
                        final IHyperlink hyperlink = new IHyperlink() {

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
                                ReviewContentSummaryView.this.controller.onClick(link);
                            }
                        };
                        return new IHyperlink[] { hyperlink };
                    }
                }
                return null;
            }
        };
        this.viewer.setHyperlinkDetectors(new IHyperlinkDetector[] { linkDetector }, 0);

        final Display display = Display.getCurrent();
        final Color color = display.getSystemColor(SWT.COLOR_BLUE);
        this.viewer.setHyperlinkPresenter(new DefaultHyperlinkPresenter(color));

        this.hookContextMenu();

        this.commitsListener = new CommitsInReviewListener() {

            // SWTException: Invalid thread access, if not using asyncExec.
            @Override
            public void notifyCommits(List<ICommit> currentCommits) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        ReviewContentSummaryView.this.controller.processCommits(currentCommits);
                    }
                });
            }
        };
        ReviewUi.registerCommitsInReviewListener(this.commitsListener);

        if (!ReviewUi.isIdle()) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    ReviewContentSummaryView.this.controller.processCommits(ReviewUi.getCommitsInReview());
                }
            });
        }
    }

    private void hookContextMenu() {
        final MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        final Menu menu = menuMgr.createContextMenu(this.viewer.getControl());
        this.viewer.getControl().setMenu(menu);
        this.getSite().registerContextMenu(menuMgr, this.viewer);
    }

    @Override
    public void setFocus() {
        this.viewer.getControl().setFocus();
    }

    /**
     * Update text to show.
     */
    public void setText(String text) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                final int i = ReviewContentSummaryView.this.viewer.getTopIndex();
                ReviewContentSummaryView.this.viewer.getDocument().set(text);
                ReviewContentSummaryView.this.viewer.setTopIndex(i);
            }
        });
    }

    /**
     * Set and mark up hyperlinks regions.
     */
    public void setHyperLinks(List<SummaryTextPart> parts) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                ReviewContentSummaryView.this.setHyperLinksInternal(parts);
            }
        });
    }

    private void setHyperLinksInternal(List<SummaryTextPart> parts) {
        this.links = parts;

        final StyledText text = this.viewer.getTextWidget();
        if (text == null || text.isDisposed()) {
            return;
        }

        for (final SummaryTextPart part : parts) {
            for (final StyleRange styleRange : part.getStyleRanges()) {
                text.setStyleRange(styleRange);
            }
        }
    }
}
