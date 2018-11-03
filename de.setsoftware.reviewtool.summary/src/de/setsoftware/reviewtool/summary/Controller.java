package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;

import de.setsoftware.reviewtool.model.api.ICommit;

/**
 * Controls commits processing and manages all needed object for summary
 * generation and presentation.
 */
public class Controller {
    public static final int SUMMARY_LENGTH = 20;
    public static final int REFDIFF_LENGTH = SUMMARY_LENGTH / 2;
    public static final int DELTADOC_LENGTH = SUMMARY_LENGTH / 2;

    private final ReviewContentSummaryView view;
    private List<SummaryTextPart> summary;
    private ChangePartsModel model;

    /**
     * Create controller for given view.
     */
    public Controller(ReviewContentSummaryView view) {
        this.view = view;
    }

    /**
     * Process given list of commits and generate summary using different summary
     * techniques. Generated summary will be automatically presented in view.
     */
    public synchronized void processCommits(List<? extends ICommit> commits) {
        this.view.setText("Generating summary...");
        this.view.setHyperLinks(new ArrayList<>());
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                Controller.this.processCommitsIntrnal(commits);
            }
        };
        final Thread t = new Thread(r);
        t.start();
    }

    @SuppressWarnings("deprecation")
    private synchronized void processCommitsIntrnal(List<? extends ICommit> commits) {
        this.model = new ChangePartsModel();
        String refDiff = "";
        final StringBuilder deltaDoc = new StringBuilder();
        for (final ICommit commit : commits) {
            try {
                final CommitParser parser = new CommitParser(commit, this.model);
                parser.processCommit();
                refDiff = refDiff + RefDiffTechnique.process(parser.previousDir, parser.currentDir,
                        parser.previousDirFiles, parser.currentDirFiles, this.model);

                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        deltaDoc.append(DeltaDocTechnique.process(parser.previousDir, parser.currentDir,
                                parser.previousDirFiles, parser.currentDirFiles, Controller.this.model));
                    }
                };
                final Thread t = new Thread(r);
                t.start();
                int sleeps = 10;
                while (t.isAlive() && sleeps > 0) {
                    sleeps--;
                    Thread.sleep(1000);
                }
                t.stop();

                parser.clean();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        this.model.sort();
        this.summary = new ArrayList<>();

        if (!refDiff.equals("")) {
            refDiff = "Detected refactorings:\n" + refDiff;
            final SummaryTextPart part = new SummaryTextPart(refDiff);
            this.summary.add(part);
        }

        this.summary.addAll(SummaryTextGenerator.generateSummary(this.model));

        if (!deltaDoc.toString().equals("")) {
            final SummaryTextPart part = new SummaryTextPart(deltaDoc.toString());
            this.summary.add(part);
        }
        SummaryTextGenerator.addLinks(this.summary, SUMMARY_LENGTH);
        this.updateText();
    }

    /**
     * Start action triggered by clicking on given hyperlink region.
     */
    public synchronized void onClick(IRegion link) {
        final SummaryTextPart part = (SummaryTextPart) link;
        part.toggleFolded();
        this.updateText();
    }

    private void updateText() {
        final String text = SummaryTextGenerator.getText(this.summary);
        this.view.setText(text);
        this.view.setHyperLinks(this.summary);
    }
}
