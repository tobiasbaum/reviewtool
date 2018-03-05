package de.setsoftware.reviewtool.summary;

import java.util.List;

import org.eclipse.jface.text.IRegion;

import de.setsoftware.reviewtool.model.api.ICommit;

/**
 * Controls commits processing and manages all needed object for summary
 * generation and presentation.
 */
public class Controller {
    ReviewContentSummaryView view;
    List<SummaryPart> summary;
    ChangePartsModel model;

    public Controller(ReviewContentSummaryView view) {
        this.view = view;
    }

    synchronized public void processCommits(List<? extends ICommit> commits) {
        model = new ChangePartsModel();
        String refDiff = "";
        for (ICommit commit : commits) {
            try {
                CommitParser parser = new CommitParser(commit, model);
                parser.processCommit();
                refDiff = refDiff + RefDiffTechnique.process(parser.previousDir, parser.currentDir,
                        parser.previousDirFiles, parser.currentDirFiles, model);
                parser.clean();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        model.sort();
        summary = TextGenerator.generateText(model);
        if (!refDiff.equals("")) {
            refDiff = "Detected refactorings:\n" + refDiff;
            SummaryPart part = new SummaryPart();
            part.textFolded = refDiff;
            summary.add(part);
        }
        updateText();
    }

    synchronized public void onClick(IRegion link) {
        SummaryPart part = (SummaryPart) link;
        part.folded = !part.folded;
        TextGenerator.updateLinkRegions(summary);
        updateText();
    }

    private void updateText() {
        String text = TextGenerator.getText(summary);
        view.setText(text);
        view.setHyperLinks(summary);
    }
}
