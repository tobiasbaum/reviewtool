package de.setsoftware.reviewtool.model.changestructure;

import java.util.Date;
import java.util.List;

import de.setsoftware.reviewtool.base.IPartiallyComparable;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.api.IUnknownRevision;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Factory for the various classes belonging to the changestructure.
 * Allows (future) decoupling of the changesources from the concrete implementations.
 */
public class ChangestructureFactory {

    public static Commit createCommit(
            final IWorkingCopy wc,
            final String message,
            final List<? extends IChange> changes,
            final IRevision revision,
            final Date timestamp) {
        return new Commit(wc, message, changes, revision, timestamp);
    }

    public static IBinaryChange createBinaryChange(
            final IWorkingCopy wc,
            final IRevisionedFile from,
            final IRevisionedFile to,
            final boolean irrelevantForReview) {
        return new BinaryChange(wc, from, to, irrelevantForReview);
    }

    public static ITextualChange createTextualChangeHunk(
            final IWorkingCopy wc,
            final IFragment from,
            final IFragment to,
            final boolean irrelevantForReview) {
        return new TextualChangeHunk(wc, from, to, irrelevantForReview);
    }

    public static IRevisionedFile createFileInRevision(final String path, final IRevision revision) {
        return new FileInRevision(path, revision);
    }

    public static IFragment createFragment(
            final IRevisionedFile file,
            final IPositionInText from,
            final IPositionInText to) {
        return new Fragment(file, from, to);
    }

    public static IHunk createHunk(final IFragment from, final IFragment to) {
        return new Hunk(from, to);
    }

    public static ILocalRevision createLocalRevision(final IWorkingCopy wc) {
        return new LocalRevision(wc);
    }

    public static <R extends IPartiallyComparable<R>> IRepoRevision<R> createRepoRevision(
            final R id,
            final IRepository repo) {
        return new RepoRevision<>(id, repo);
    }

    public static IUnknownRevision createUnknownRevision(final IRepository repo) {
        return new UnknownRevision(repo);
    }

    public static IPositionInText createPositionInText(final int line, final int column) {
        return new PositionInText(line, column);
    }

    public static IChangeData createChangeData(
            final IChangeSource source,
            final List<? extends ICommit> commits) {
        return new ChangeData(source, commits);
    }
}
