package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates text from default representation of a commits and manages links.
 */
public class SummaryTextGenerator {
    /**
     * Generate text parts for the given model.
     */
    public static List<SummaryTextPart> generateSummary(ChangePartsModel model) {
        List<SummaryTextPart> summary = new ArrayList<>();

        addPart(getNewTypes(model), summary);
        addPart(getChangedTypes(model), summary);
        addPart(getDeletedTypes(model), summary);

        addPart(getNewMethods(model), summary);
        addPart(getChangedMethods(model), summary);
        addPart(getDeletedMethods(model), summary);

        addPart(getNewFiles(model), summary);
        addPart(getChangedFiles(model), summary);
        addPart(getDeletedFiles(model), summary);

        return summary;
    }

    /**
     * Generate text from a list of summary parts with respect of folding status.
     */
    public static String getText(List<SummaryTextPart> summary) {
        updateLinkRegions(summary);
        String text = "";
        for (SummaryTextPart part : summary) {
            if (part.folded) {
                text = text + part.textFolded;
            } else {
                text = text + part.text;
            }
        }
        return text;
    }

    private static void updateLinkRegions(List<SummaryTextPart> summary) {
        int offset = 0;
        for (SummaryTextPart part : summary) {
            if (part.folded) {
                offset = offset + part.textFolded.length();
            } else {
                offset = offset + part.text.length();
            }
            if (part.hasLink) {
                part.linkOffset = offset - part.linkLength - 2;
            }
        }
    }

    private static void addPart(SummaryTextPart part, List<SummaryTextPart> summary) {
        if (part.lines != 1) {
            summary.add(part);
        }
    }

    /**
     * Add hyperlinks regions to and fold summary parts whose text is to long.
     */
    public static void addLinks(List<SummaryTextPart> summary) {
        for (SummaryTextPart part : summary) {
            addLinkIfNeeded(part);
        }
    }

    private static void addLinkIfNeeded(SummaryTextPart part) {
        if (part.lines > part.maxLinesFolded) {
            String[] text = part.text.split("\n");
            for (int i = 0; i < part.maxLinesFolded - 1; i++) {
                part.textFolded = part.textFolded + text[i] + "\n";
                part.linesFolded++;
            }
            part.hasLink = true;
            part.folded = true;
            part.linkLength = 7; // Link length (more.. or less...)
            part.text = part.text + "less..." + "\n\n";
            part.textFolded = part.textFolded + "(" + (part.lines - part.linesFolded) + " more lines) show...\n\n";
        } else {
            part.text = part.text + "\n";
        }
    }

    private static SummaryTextPart getNewTypes(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "New types:\n";
        part.lines++;
        addChangeParts(part, model.newParts.types);
        return part;
    }

    private static SummaryTextPart getChangedTypes(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "Changed types:\n";
        part.lines++;
        addChangeParts(part, model.changedParts.types);
        return part;
    }

    private static SummaryTextPart getDeletedTypes(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "Deleted types:\n";
        part.lines++;
        addChangeParts(part, model.deletedParts.types);
        return part;
    }

    private static SummaryTextPart getNewMethods(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "New methods:\n";
        part.lines++;
        addChangeParts(part, model.newParts.methods);
        return part;
    }

    private static SummaryTextPart getChangedMethods(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "Changed methods:\n";
        part.lines++;
        addChangeParts(part, model.changedParts.methods);
        return part;
    }

    private static SummaryTextPart getDeletedMethods(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "Deleted methods:\n";
        part.lines++;
        addChangeParts(part, model.deletedParts.methods);
        return part;
    }

    private static SummaryTextPart getNewFiles(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "New non source files:\n";
        part.lines++;
        addChangeParts(part, model.newParts.files);
        return part;
    }

    private static SummaryTextPart getChangedFiles(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "Changed non source files:\n";
        part.lines++;
        addChangeParts(part, model.changedParts.files);
        return part;
    }

    private static SummaryTextPart getDeletedFiles(ChangePartsModel model) {
        SummaryTextPart part = new SummaryTextPart();
        part.text = "Deleted non source files:\n";
        part.lines++;
        addChangeParts(part, model.deletedParts.files);
        return part;
    }

    private static void addChangeParts(SummaryTextPart part, List<ChangePart> changes) {
        for (ChangePart change : changes) {
            part.text = part.text + change.toString() + "\n";
            part.lines++;
        }
    }
}
