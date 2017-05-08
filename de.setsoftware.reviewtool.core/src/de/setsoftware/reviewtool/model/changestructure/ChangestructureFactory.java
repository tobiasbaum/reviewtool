package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.ITextualChange;

/**
 * Factory for the various classes belonging to the changestructure.
 * Allows (future) decoupling of the changesources from the concrete implementations.
 */
public class ChangestructureFactory {

    public static Commit createCommit(String message, List<? extends IChange> changes, final boolean isVisible) {
        return new Commit(message, changes, isVisible);
    }

    public static IBinaryChange createBinaryChange(
            IRevisionedFile from, IRevisionedFile to, boolean irrelevantForReview, final boolean isVisible) {
        return new BinaryChange(from, to, irrelevantForReview, isVisible);
    }

    public static ITextualChange createTextualChangeHunk(
            IFragment from, IFragment to, boolean irrelevantForReview, final boolean isVisible) {
        return new TextualChangeHunk(from, to, irrelevantForReview, isVisible);
    }

    public static IRevisionedFile createFileInRevision(String path, IRevision revision, IRepository repository) {
        return new FileInRevision(path, revision, repository);
    }

    public static IFragment createFragment(IRevisionedFile file, IPositionInText from, IPositionInText to) {
        return new Fragment(file, from, to);
    }

    public static IHunk createHunk(IFragment from, IFragment to) {
        return new Hunk(from, to);
    }

    public static ILocalRevision createLocalRevision() {
        return new LocalRevision();
    }

    public static IRepoRevision createRepoRevision(Object id) {
        return new RepoRevision(id);
    }

    public static IPositionInText createPositionInText(int line, int column) {
        return new PositionInText(line, column);
    }
}
