package de.setsoftware.reviewtool.summary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.revwalk.RevCommit;

import de.setsoftware.reviewtool.summary.ChangePart.Kind;
import refdiff.core.RefDiff;
import refdiff.core.api.RefactoringType;
import refdiff.core.rm2.analysis.StructuralDiffHandler;
import refdiff.core.rm2.model.SDEntity;
import refdiff.core.rm2.model.SDModel;
import refdiff.core.rm2.model.refactoring.SDRefactoring;

public class RefDiffTechnique {
    public static String process(Path previousDir, Path currentDir, Set<Path> previousDirFiles,
            Set<Path> currentDirFiles, ChangePartsModel model) {
        ArrayList<String> filesBefore = new ArrayList<>();
        ArrayList<String> filesCurrent = new ArrayList<>();

        for (Path file : previousDirFiles) {
            filesBefore.add(previousDir.relativize(file).toString());
        }

        for (Path file : currentDirFiles) {
            filesCurrent.add(currentDir.relativize(file).toString());
        }

        StringBuilder text = new StringBuilder("");
        RefDiff refDiff = new RefDiff();
        StructuralDiffHandler handler = new StructuralDiffHandler() {
            @Override
            public void handle(RevCommit commitData, SDModel sdModel) {
                for (SDRefactoring ref : sdModel.getRefactorings()) {
                    if (ref.getRefactoringType() == RefactoringType.MOVE_CLASS
                            || ref.getRefactoringType() == RefactoringType.MOVE_RENAME_CLASS
                            || ref.getRefactoringType() == RefactoringType.RENAME_CLASS) {
                        text.append(processClassRefactoring(ref, model) + "\n");
                    } else if (ref.getRefactoringType() == RefactoringType.MOVE_OPERATION
                            || ref.getRefactoringType() == RefactoringType.CHANGE_METHOD_SIGNATURE
                            || ref.getRefactoringType() == RefactoringType.RENAME_METHOD) {
                        text.append(processMethodRefactoring(ref, model) + "\n");
                    } else {
                        text.append(ref.getRefactoringType().getDisplayName() + ": " + ref.getEntityBefore().key()
                                + " --> " + ref.getEntityAfter().key());
                    }
                }
            }
        };
        refDiff.detectAtCommit(previousDir.toFile(), currentDir.toFile(), filesBefore, filesCurrent, handler);
        return text.toString();
    }

    private static String processClassRefactoring(SDRefactoring ref, ChangePartsModel model) {
        ChangePart before = getTypePart(ref.getEntityBefore());
        ChangePart after = getTypePart(ref.getEntityAfter());

        model.deletedParts.removePart(before);
        model.newParts.removePart(after);
        removeChildren(before, model.deletedParts.methods);
        removeChildren(before, model.deletedParts.types);
        removeChildren(after, model.newParts.methods);
        removeChildren(after, model.newParts.types);

        return ref.getRefactoringType().getDisplayName() + ": " + before.toString() + " --> " + after.toString();
    }

    private static String processMethodRefactoring(SDRefactoring ref, ChangePartsModel model) {
        ChangePart before = getMethodPart(ref.getEntityBefore());
        ChangePart after = getMethodPart(ref.getEntityAfter());

        model.deletedParts.removePart(before);
        model.newParts.removePart(after);
        removeChildren(before, model.deletedParts.methods);
        removeChildren(before, model.deletedParts.types);
        removeChildren(after, model.newParts.methods);
        removeChildren(after, model.newParts.types);

        return ref.getRefactoringType().getDisplayName() + ": " + before.toString() + " --> " + after.toString();
    }

    private static ChangePart getTypePart(SDEntity entity) {
        String name = entity.simpleName();
        String parent = entity.key().toString().replaceAll(".*/", "").replaceFirst("\\.[^.]*$", "");
        return new ChangePart(name, parent, Kind.TYPE);
    }

    private static ChangePart getMethodPart(SDEntity entity) {
        String name = entity.simpleName();
        String parent = entity.key().toString().replaceAll(".*/", "").replaceFirst("#[^#]*$", "");
        return new ChangePart(name, parent, Kind.METHOD);
    }

    private static void removeChildren(ChangePart parent, List<ChangePart> parts) {
        Iterator<ChangePart> partsIterator = parts.iterator();
        while (partsIterator.hasNext()) {
            ChangePart part = partsIterator.next();
            if (isChild(part, parent)) {
                partsIterator.remove();
            }
        }
    }

    private static boolean isChild(ChangePart part, ChangePart parent) {
        String parentString = parent.getParent() + CommitParser.MEMBER_SEPARATOR + parent.getName();
        return part.getParent().contains(parentString);
    }
}
