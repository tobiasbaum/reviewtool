package de.setsoftware.reviewtool.summary;

import java.io.File;
import java.util.ArrayList;
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
	public static String process(File previousDir, File currentDir, Set<String> previousDirFiles,
			Set<String> currentDirFiles, ChangePartsModel model) {
		ArrayList<String> filesBefore = new ArrayList<>();
		ArrayList<String> filesCurrent = new ArrayList<>();

		for (String file : previousDirFiles)
			filesBefore.add(file.replaceFirst(previousDir.getPath(), ""));

		for (String file : currentDirFiles)
			filesCurrent.add(file.replaceFirst(currentDir.getPath(), ""));

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
		refDiff.detectAtCommit(previousDir, currentDir, filesBefore, filesCurrent, handler);
		return text.toString();
	}

	private static String processClassRefactoring(SDRefactoring ref, ChangePartsModel model) {
		ChangePart before = getTypePart(ref.getEntityBefore());
		ChangePart after = getTypePart(ref.getEntityAfter());

		model.deletedParts.removePart(before);
		model.newParts.removePart(after);

		return ref.getRefactoringType().getDisplayName() + ": " + before.toString() + " --> " + after.toString();
	}

	private static String processMethodRefactoring(SDRefactoring ref, ChangePartsModel model) {
		ChangePart before = getMethodPart(ref.getEntityBefore());
		ChangePart after = getMethodPart(ref.getEntityAfter());

		model.deletedParts.removePart(before);
		model.newParts.removePart(after);

		return ref.getRefactoringType().getDisplayName() + ": " + before.toString() + " --> " + after.toString();
	}

	private static ChangePart getTypePart(SDEntity entity) {
		String name = entity.simpleName();
		String parent = entity.key().toString().replaceAll(".*/", "").replaceFirst("\\..*$", "");
		return new ChangePart(name, parent, Kind.TYPE);
	}

	private static ChangePart getMethodPart(SDEntity entity) {
		String name = entity.simpleName();
		String parent = entity.key().toString().replaceAll(".*/", "").replaceFirst("#.*$", "");
		return new ChangePart(name, parent, Kind.METHOD);
	}
}
