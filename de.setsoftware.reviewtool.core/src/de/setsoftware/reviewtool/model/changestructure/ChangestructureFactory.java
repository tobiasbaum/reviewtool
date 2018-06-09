package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

/**
 * Factory for the various classes belonging to the changestructure.
 * Allows (future) decoupling of the changesources from the concrete implementations.
 */
public class ChangestructureFactory {

    public static Commit createCommit(
            final String message,
            final List<? extends IChange> changes,
            final IRevision revision,
            final Date timestamp) {
        return new Commit(message, changes, revision, timestamp);
    }

    public static IBinaryChange createBinaryChange(
            IRevisionedFile from, IRevisionedFile to, boolean irrelevantForReview) {
        return new BinaryChange(from, to, irrelevantForReview);
    }

    public static ITextualChange createTextualChangeHunk(
            IFragment from, IFragment to, boolean irrelevantForReview) {
        return new TextualChangeHunk(from, to, irrelevantForReview);
    }

    public static IRevisionedFile createFileInRevision(final String path, final IRevision revision) {
        return new FileInRevision(path, revision);
    }

    public static IFragment createFragment(IRevisionedFile file, IPositionInText from, IPositionInText to) {
        return new Fragment(file, from, to);
    }

    public static IHunk createHunk(IFragment from, IFragment to) {
        return new Hunk(from, to);
    }

    public static ILocalRevision createLocalRevision(final IRepository repo) {
        return new LocalRevision(repo);
    }

    public static IRepoRevision createRepoRevision(final Object id, final IRepository repo) {
        return new RepoRevision(id, repo);
    }

    public static IUnknownRevision createUnknownRevision(final IRepository repo) {
        return new UnknownRevision(repo);
    }

    public static IPositionInText createPositionInText(int line, int column) {
        return new PositionInText(line, column);
    }

    public static IChangeData createChangeData(
            final IChangeSource source,
            final List<? extends ICommit> commits) {
        return new ChangeData(source, commits);
    }

    public static IChangeData createChangeData(
            final IChangeSource source,
            final List<? extends ICommit> commits,
            final Map<File, IRevisionedFile> localPathMap) {
        return new ChangeData(source, commits, localPathMap);
    }
}
