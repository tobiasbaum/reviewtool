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

    private ReviewContentSummaryView view;
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
        Runnable r = new Runnable() {
            @Override
            public void run() {
                processCommitsIntrnal(commits);
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    @SuppressWarnings("deprecation")
    private synchronized void processCommitsIntrnal(List<? extends ICommit> commits) {
        this.model = new ChangePartsModel();
        int freeLength = SUMMARY_LENGTH;
        String refDiff = "";
        StringBuilder deltaDoc = new StringBuilder();
        for (ICommit commit : commits) {
            try {
                CommitParser parser = new CommitParser(commit, this.model);
                parser.processCommit();
                refDiff = refDiff + RefDiffTechnique.process(parser.previousDir, parser.currentDir,
                        parser.previousDirFiles, parser.currentDirFiles, this.model);

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        deltaDoc.append(DeltaDocTechnique.process(parser.previousDir, parser.currentDir,
                                parser.previousDirFiles, parser.currentDirFiles, Controller.this.model));
                    }
                };
                Thread t = new Thread(r);
                t.start();
                int sleeps = 10;
                while (t.isAlive() && sleeps > 0) {
                    sleeps--;
                    Thread.sleep(1000);
                }
                t.stop();

                parser.clean();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.model.sort();
        this.summary = SummaryTextGenerator.generateSummary(this.model);
        for (SummaryTextPart p : this.summary) {
            p.maxLinesFolded = freeLength / this.summary.size() + 2;
        }

        if (!refDiff.equals("")) {
            refDiff = "Detected refactorings:\n" + refDiff;
            SummaryTextPart part = new SummaryTextPart();
            part.text = refDiff;
            part.lines = refDiff.split("\n").length;
            part.maxLinesFolded = REFDIFF_LENGTH;
            part.linesFolded = Math.min(part.lines, part.maxLinesFolded);
            freeLength = freeLength - part.linesFolded;
            for (SummaryTextPart p : this.summary) {
                p.maxLinesFolded = freeLength / this.summary.size() + 2;
            }
            this.summary.add(part);
        }

        if (!deltaDoc.toString().equals("")) {
            SummaryTextPart part = new SummaryTextPart();
            part.text = deltaDoc.toString();
            part.lines = deltaDoc.toString().split("\n").length;
            part.maxLinesFolded = DELTADOC_LENGTH;
            part.linesFolded = Math.min(part.lines, part.maxLinesFolded);
            freeLength = freeLength - part.linesFolded;
            for (SummaryTextPart p : this.summary) {
                p.maxLinesFolded = freeLength / this.summary.size() + 2;
            }
            this.summary.add(part);
        }
        SummaryTextGenerator.addLinks(this.summary);
        updateText();
    }

    /**
     * Start action triggered by clicking on given hyperlink region.
     */
    public synchronized void onClick(IRegion link) {
        SummaryTextPart part = (SummaryTextPart) link;
        part.folded = !part.folded;
        updateText();
    }

    private void updateText() {
        String text = SummaryTextGenerator.getText(this.summary);
        this.view.setText(text);
        this.view.setHyperLinks(this.summary);
    }
}
