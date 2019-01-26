package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import de.setsoftware.reviewtool.model.api.AbstractChangeSource;
import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * A change source that loads the changes from Git.
 */
public class GitChangeSource extends AbstractChangeSource {

    /**
     * Constructor.
     */
    GitChangeSource(
            final String logMessagePattern,
            final long maxTextDiffThreshold,
            final int logCacheMinSize) {
        super(logMessagePattern, maxTextDiffThreshold);

        GitRepositoryManager.getInstance().init(logCacheMinSize);
    }

    @Override
    public IChangeData getRepositoryChanges(final String key, final IChangeSourceUi ui)
        throws ChangeSourceException {

        try {
            ui.subTask("Determining relevant commits...");
            final List<GitRevision> revisions = this.determineRelevantRevisions(key, ui);
            ui.subTask("Analyzing commits...");
            final List<ICommit> commits = this.convertRepoRevisionsToChanges(revisions, ui);
            return ChangestructureFactory.createChangeData(this, commits);
        } catch (final IOException | GitAPIException e) {
            throw new ChangeSourceException(this, e);
        }
    }

    private List<GitRevision> determineRelevantRevisions(
            final String key,
            final IChangeSourceUi ui) throws GitAPIException, IOException {

        final Pattern pattern = this.createPatternForKey(key);
        final Predicate<GitRevision> handler = (final GitRevision logEntry) -> {
            final String message = logEntry.getMessage();
            return message != null && pattern.matcher(message).matches();
        };

        return GitWorkingCopyManager.getInstance().traverseEntries(handler, ui);
    }

    private List<ICommit> convertRepoRevisionsToChanges(
            final List<GitRevision> revisions,
            final IProgressMonitor ui) {
        final List<ICommit> ret = new ArrayList<>();
        for (final GitRevision e : revisions) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }
            this.convertToCommitIfPossible(e, ret, ui);
        }
        return ret;
    }

    private void convertToCommitIfPossible(
            final GitRevision e,
            final Collection<? super ICommit> result,
            final IProgressMonitor ui) {
        //        final List<? extends IChange> changes = this.determineChangesInCommit(wc, e, ui);
        final List<? extends IChange> changes = Collections.singletonList(null);
        if (!changes.isEmpty()) {
            result.add(ChangestructureFactory.createCommit(
                    e.getWorkingCopy(),
                    e.toPrettyString(),
                    changes,
                    e.toRevision(),
                    e.getDate()));
        }
    }

    @Override
    public void analyzeLocalChanges(List<File> relevantPaths) throws ChangeSourceException {
        // TODO implement analyzeLocalChanges()
    }

    @Override
    public File determineWorkingCopyRoot(final File projectRoot) throws ChangeSourceException {
        try {
            return new FileRepositoryBuilder().findGitDir(projectRoot).build().getDirectory();
        } catch (final IOException ex) {
            throw new ChangeSourceException(this, ex);
        }
    }

    @Override
    protected void workingCopyAdded(File wcRoot) {
        GitWorkingCopyManager.getInstance().getWorkingCopy(wcRoot);
    }

    @Override
    protected void workingCopyRemoved(File wcRoot) {
        GitWorkingCopyManager.getInstance().removeWorkingCopy(wcRoot);
    }

    @Override
    public void clearCaches() {
        // TODO Auto-generated method stub

    }

}
