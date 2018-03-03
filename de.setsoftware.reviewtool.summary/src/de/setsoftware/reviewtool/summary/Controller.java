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

	public void processCommits(List<? extends ICommit> commits) {
		model = new ChangePartsModel();
		for (ICommit commit : commits) {
			try {
				CommitParser parser = new CommitParser(commit, model);
				parser.processCommit();
				parser.clean();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		summary = TextGenerator.generateText(model);
		updateText();
	}

	public void onClick(IRegion link) {
		SummaryPart part = (SummaryPart) link;
		part.folded = !part.folded;
		TextGenerator.updateLinkRegion(summary);
		updateText();
	}

	private void updateText() {
		String text = TextGenerator.getText(summary);
		view.setText(text);
		view.setHyperLinks(summary);
	}
}
