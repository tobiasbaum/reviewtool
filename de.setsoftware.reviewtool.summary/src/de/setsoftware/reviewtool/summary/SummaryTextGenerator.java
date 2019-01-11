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
        final List<SummaryTextPart> summary = new ArrayList<>();

        for (final String sourceFolder : model.getSourceFolders()) {
            addPart(getNewTypes(model, sourceFolder), summary);
            addPart(getChangedTypes(model, sourceFolder), summary);
            addPart(getDeletedTypes(model, sourceFolder), summary);

            addPart(getNewMethods(model, sourceFolder), summary);
            addPart(getChangedMethods(model, sourceFolder), summary);
            addPart(getDeletedMethods(model, sourceFolder), summary);
        }

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
        final StringBuilder text = new StringBuilder();;
        for (final SummaryTextPart part : summary) {
            text.append(part.getVisibleText());
        }
        return text.toString();
    }

    private static void updateLinkRegions(List<SummaryTextPart> summary) {
        int offset = 0;
        for (final SummaryTextPart part : summary) {
            offset += part.getVisibleTextLength();
            part.updateOffsets(offset);
        }
    }

    private static void addPart(SummaryTextPart part, List<SummaryTextPart> summary) {
        if (part.getLineCount() > 1) {
            summary.add(part);
        }
    }

    /**
     * Add hyperlinks regions to and fold summary parts whose text is to long.
     */
    public static void addLinks(List<SummaryTextPart> summary, int maxLength) {
        int remaining = maxLength;

        for (int i = summary.size() - 1; i >= 0; i--) {
            final SummaryTextPart part = summary.get(i);
            part.addLinkIfNeeded(remaining / (i + 1));
            remaining -= part.getVisibleLineCount();
        }
    }

    private static SummaryTextPart getNewTypes(ChangePartsModel model, String sourceFolder) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("New types (" + sourceFolder + "):"));
        addChangeParts(part, model.newParts.types.get(sourceFolder));
        return part;
    }

    private static SummaryTextPart getChangedTypes(ChangePartsModel model, String sourceFolder) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("Changed types (" + sourceFolder + "):"));
        addChangeParts(part, model.changedParts.types.get(sourceFolder));
        return part;
    }

    private static SummaryTextPart getDeletedTypes(ChangePartsModel model, String sourceFolder) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("Deleted types (" + sourceFolder + "):"));
        addChangeParts(part, model.deletedParts.types.get(sourceFolder));
        return part;
    }

    private static SummaryTextPart getNewMethods(ChangePartsModel model, String sourceFolder) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("New methods (" + sourceFolder + "):"));
        addChangeParts(part, model.newParts.methods.get(sourceFolder));
        return part;
    }

    private static SummaryTextPart getChangedMethods(ChangePartsModel model, String sourceFolder) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("Changed methods (" + sourceFolder + "):"));
        addChangeParts(part, model.changedParts.methods.get(sourceFolder));
        return part;
    }

    private static SummaryTextPart getDeletedMethods(ChangePartsModel model, String sourceFolder) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("Deleted methods (" + sourceFolder + "):"));
        addChangeParts(part, model.deletedParts.methods.get(sourceFolder));
        return part;
    }

    private static SummaryTextPart getNewFiles(ChangePartsModel model) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("New non source files:"));
        addChangeParts(part, model.newParts.files);
        return part;
    }

    private static SummaryTextPart getChangedFiles(ChangePartsModel model) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("Changed non source files:"));
        addChangeParts(part, model.changedParts.files);
        return part;
    }

    private static SummaryTextPart getDeletedFiles(ChangePartsModel model) {
        final SummaryTextPart part = new SummaryTextPart();
        part.addLine(TextWithStyles.italic("Deleted non source files:"));
        addChangeParts(part, model.deletedParts.files);
        return part;
    }

    private static void addChangeParts(SummaryTextPart part, List<ChangePart> changes) {
        for (final ChangePart change : changes) {
            part.addLine(change.toStyledText());
        }
    }
}
