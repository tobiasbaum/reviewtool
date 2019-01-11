package de.setsoftware.reviewtool.summary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;

import de.setsoftware.reviewtool.summary.ChangePart.Kind;
import refdiff.core.api.RefactoringType;
import refdiff.core.rm2.analysis.RefDiffConfigImpl;
import refdiff.core.rm2.analysis.SDModelBuilder;
import refdiff.core.rm2.model.SDEntity;
import refdiff.core.rm2.model.SDModel;
import refdiff.core.rm2.model.refactoring.SDRefactoring;

/**
 * RefDiff summary technique.
 */
public class RefDiffTechnique {
    /**
     * Detect refactorings summary using RefDiff and remove recognized refactorings
     * from default model.
     */
    public static String process(Path previousDir, Path currentDir, Set<Path> previousDirFiles,
            Set<Path> currentDirFiles, ChangePartsModel model) {
        final ArrayList<String> filesBefore = new ArrayList<>();
        final ArrayList<String> filesCurrent = new ArrayList<>();

        for (final Path file : previousDirFiles) {
            filesBefore.add(previousDir.relativize(file).toString());
        }

        for (final Path file : currentDirFiles) {
            filesCurrent.add(currentDir.relativize(file).toString());
        }

        final RefDiffConfigImpl config = new RefDiffConfigImpl();
        final SDModelBuilder builder = new SDModelBuilder(config);
        builder.analyzeAfter(currentDir.toFile(), filesCurrent);
        builder.analyzeBefore(previousDir.toFile(), filesBefore);
        final SDModel sdModel = builder.buildModel();

        final StringBuilder text = new StringBuilder("");
        for (final SDRefactoring ref : sdModel.getRefactorings()) {
            if (ref.getRefactoringType() == RefactoringType.MOVE_CLASS
                    || ref.getRefactoringType() == RefactoringType.MOVE_RENAME_CLASS
                    || ref.getRefactoringType() == RefactoringType.RENAME_CLASS) {
                text.append(processClassRefactoring(ref, model) + "\n");
            } else if (ref.getRefactoringType() == RefactoringType.MOVE_OPERATION
                    || ref.getRefactoringType() == RefactoringType.CHANGE_METHOD_SIGNATURE
                    || ref.getRefactoringType() == RefactoringType.RENAME_METHOD) {
                text.append(processMethodRefactoring(ref, model) + "\n");
            } else {
                text.append(ref.getRefactoringType().getDisplayName() + ": " + ref.getEntityBefore().key() + " --> "
                        + ref.getEntityAfter().key());
            }
        }

        return text.toString();
    }

    private static String processClassRefactoring(SDRefactoring ref, ChangePartsModel model) {
        final ChangePart before = getTypePart(ref.getEntityBefore());
        final ChangePart after = getTypePart(ref.getEntityAfter());

        model.deletedParts.removePart(before);
        model.newParts.removePart(after);
        removeChildren(before, model.deletedParts);
        removeChildren(after, model.newParts);

        return ref.getRefactoringType().getDisplayName() + ": " + before.toString() + " --> " + after.toString();
    }

    private static String processMethodRefactoring(SDRefactoring ref, ChangePartsModel model) {
        final ChangePart before = getMethodPart(ref.getEntityBefore());
        final ChangePart after = getMethodPart(ref.getEntityAfter());

        model.deletedParts.removePart(before);
        model.newParts.removePart(after);
        removeChildren(before, model.deletedParts);
        removeChildren(after, model.newParts);

        return ref.getRefactoringType().getDisplayName() + ": " + before.toString() + " --> " + after.toString();
    }

    private static ChangePart getTypePart(SDEntity entity) {
        final String name = entity.simpleName();
        final String parent = entity.key().toString().replaceAll(".*/", "").replaceFirst("\\.[^.]*$", "");
        return new ChangePart(name, parent, toSourceFolder(entity), Kind.TYPE);
    }

    private static ChangePart getMethodPart(SDEntity entity) {
        final String name = entity.simpleName();
        final String parent = entity.key().toString().replaceAll(".*/", "").replaceFirst("#[^#]*$", "");
        return new ChangePart(name, parent, toSourceFolder(entity), Kind.METHOD);
    }

    private static String toSourceFolder(SDEntity entity) {
        return CommitParser.determineSourceFolder(entity.key().toString());
    }

    private static void removeChildren(ChangePart parent, ChangeParts parts) {
        for (final ChangePart part : parts.getAllMethodParts()) {
            if (isChild(part, parent)) {
                parts.removePart(part);
            }
        }
        for (final ChangePart part : parts.getAllTypeParts()) {
            if (isChild(part, parent)) {
                parts.removePart(part);
            }
        }
    }

    private static boolean isChild(ChangePart part, ChangePart parent) {
        final String parentString = parent.getParent() + CommitParser.MEMBER_SEPARATOR + parent.getName();
        return part.getParent().contains(parentString);
    }
}
