package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;

import de.setsoftware.reviewtool.summary.ChangePart.Kind;

class SummaryPart implements IRegion {
	String text;
	String textFolded;
	int lines;
	int linesFolded;
	int maxLinesFolded = 4;
	boolean folded = true;
	boolean hasLink = false; // No hyper-link, if content is short enough

	int linkOffset = 0; // Character offset of whole summary text, used for displaying links
	int linkLength = 0;

	@Override
	public int getLength() {
		return linkLength;
	}

	@Override
	public int getOffset() {
		return linkOffset;
	}
}

/**
 * Generates text from default representation of a commits and manages links.
 */
public class TextGenerator {
	static public List<SummaryPart> generateText(ChangePartsModel model) {
		List<SummaryPart> summary = new ArrayList<>();
		
		addPart(getNewTypes(model), summary);
		addPart(getChangedTypes(model), summary);
		addPart(getDeletedTypes(model), summary);

		addPart(getNewMethods(model), summary);
		addPart(getChangedMethods(model), summary);
		addPart(getDeletedMethods(model), summary);

		addPart(getNewFiles(model), summary);
		addPart(getChangedFiles(model), summary);
		addPart(getDeletedFiles(model), summary);

		for (SummaryPart part : summary) {
			addLinkIfNeeded(part);
		}
		updateLinkRegions(summary);

		return summary;
	}

	static public String getText(List<SummaryPart> summary) {
		String text = "";
		for (SummaryPart part : summary) {
			if (part.folded)
				text = text + part.textFolded;
			else
				text = text + part.text;
		}
		return text;
	}

	static public void updateLinkRegions(List<SummaryPart> summary) {
		int offset = 0;
		for (SummaryPart part : summary) {
			if (part.folded)
				offset = offset + part.textFolded.length();
			else
				offset = offset + part.text.length();
			if (part.hasLink)
				part.linkOffset = offset - part.linkLength - 2;
		}
	}

	private static void addPart(SummaryPart part, List<SummaryPart> summary) {
		if (part.lines != 1)
			summary.add(part);
	}

	private static void addLinkIfNeeded(SummaryPart part) {
		if (part.lines > part.linesFolded + 1) {
			part.hasLink = true;
			part.linkLength = 7; // Link length (more.. or less...)
			part.text = part.text + "less..." + "\n\n";
			part.textFolded = part.textFolded + "(other " + (part.lines - part.linesFolded) + " items) show...\n\n";
		} else {
			part.hasLink = false;
			part.linkOffset = 0;
			part.linkLength = 0; // Links with length 0 are not presented 
			part.textFolded = part.textFolded + "\n";
		}
	}
	
	private static SummaryPart getNewTypes(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "New types:\n";
		part.lines++;
		addChangeParts(part, model.newParts.types);
		return part;
	}

	private static SummaryPart getChangedTypes(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "Changed types:\n";
		part.lines++;
		addChangeParts(part, model.changedParts.types);
		return part;
	}

	private static SummaryPart getDeletedTypes(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "Deleted types:\n";
		part.lines++;
		addChangeParts(part, model.deletedParts.types);
		return part;
	}

	private static SummaryPart getNewMethods(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "New methods:\n";
		part.lines++;
		addChangeParts(part, model.newParts.methods);
		return part;
	}

	private static SummaryPart getChangedMethods(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "Changed methods:\n";
		part.lines++;
		addChangeParts(part, model.changedParts.methods);
		return part;
	}

	private static SummaryPart getDeletedMethods(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "Deleted methods:\n";
		part.lines++;
		addChangeParts(part, model.deletedParts.methods);
		return part;
	}

	private static SummaryPart getNewFiles(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "New non source files:\n";
		part.lines++;
		addChangeParts(part, model.newParts.files);
		return part;
	}

	private static SummaryPart getChangedFiles(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "Changed non source files:\n";
		part.lines++;
		addChangeParts(part, model.changedParts.files);
		return part;
	}

	private static SummaryPart getDeletedFiles(ChangePartsModel model) {
		SummaryPart part = new SummaryPart();
		part.text = "Deleted non source files:\n";
		part.lines++;
		addChangeParts(part, model.deletedParts.files);
		return part;
	}

	private static void addChangeParts(SummaryPart part, List<ChangePart> changes) {
		for (ChangePart change : changes) {
			part.text = part.text + change.toString() + "\n";
			part.lines++;
			if (part.lines <= part.maxLinesFolded - 1) {
				part.textFolded = part.text;
				part.linesFolded = part.lines;
			}
		}
	}
}
