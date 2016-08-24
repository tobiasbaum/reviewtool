package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Factory for the various classes belonging to the changestructure.
 * Allows (future) decoupling of the changesources from the concrete implementations.
 */
public class ChangestructureFactory {

    public static Commit createCommit(String message, List<Change> changes, final boolean isVisible) {
        return new Commit(message, changes, isVisible);
    }

    public static BinaryChange createBinaryChange(FileInRevision from, FileInRevision to, boolean irrelevantForReview, final boolean isVisible) {
        return new BinaryChange(from, to, irrelevantForReview, isVisible);
    }

    public static TextualChangeHunk createTextualChangeHunk(Fragment from, Fragment to, boolean irrelevantForReview, final boolean isVisible) {
        return new TextualChangeHunk(from, to, irrelevantForReview, isVisible);
    }

    public static FileInRevision createFileInRevision(String path, Revision revision, Repository repository) {
        return new FileInRevision(path, revision, repository);
    }

    public static Fragment createFragment(FileInRevision file, PositionInText from, PositionInText to, String content) {
        return new Fragment(file, from, to, content);
    }

    public static LocalRevision createLocalRevision() {
        return new LocalRevision();
    }

    public static RepoRevision createRepoRevision(Object id) {
        return new RepoRevision(id);
    }

    public static PositionInText createPositionInText(int line, int column) {
        return new PositionInText(line, column);
    }
}
