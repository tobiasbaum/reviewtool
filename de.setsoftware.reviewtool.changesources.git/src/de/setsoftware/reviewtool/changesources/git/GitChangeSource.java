package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.AbstractChangeSource;
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
            final long maxTextDiffThreshold) {
        super(logMessagePattern, maxTextDiffThreshold);
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
        final HistoryFiller historyFiller = new HistoryFiller();
        final Predicate<GitRevision> handler = (final GitRevision logEntry) -> {
            historyFiller.register(logEntry);
            final String message = logEntry.getMessage();
            return message != null && pattern.matcher(message).matches();
        };

        final List<GitRevision> matchingEntries = GitWorkingCopyManager.getInstance().traverseEntries(handler, ui);
        historyFiller.populate(matchingEntries);
        return matchingEntries;
    }

    private List<ICommit> convertRepoRevisionsToChanges(
            final List<GitRevision> revisions,
            final IProgressMonitor ui) throws IOException {
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
            final IProgressMonitor ui) throws IOException {
        final List<? extends IChange> changes = this.determineChangesInCommit(e, ui);
        if (!changes.isEmpty()) {
            result.add(ChangestructureFactory.createCommit(
                    e.getWorkingCopy(),
                    e.toPrettyString(),
                    changes,
                    e.toRevision(),
                    e.getDate()));
        }
    }

    private List<? extends IChange> determineChangesInCommit(
            final GitRevision e,
            final IProgressMonitor ui) throws IOException {

        final List<IChange> ret = new ArrayList<>();
        final Set<String> changedPaths = e.getChangedPaths();
        final List<String> sortedPaths = new ArrayList<>(changedPaths);
        Collections.sort(sortedPaths);
        for (final String path : sortedPaths) {
            if (ui.isCanceled()) {
                throw new OperationCanceledException();
            }

            final IRevisionedFile fileInfo = ChangestructureFactory.createFileInRevision(path, e.toRevision());
            final IFileHistoryNode node = e.getWorkingCopy().getFileHistoryGraph().getNodeFor(fileInfo);
            if (node != null) {
                try {
                    ret.addAll(this.determineChangesInFile(e.getWorkingCopy(), node));
                } catch (final Exception ex) {
                    Logger.error("An error occurred while computing changes for " + fileInfo.toString(), ex);
                }
            }
        }
        return ret;
    }

    @Override
    public void analyzeLocalChanges(List<File> relevantPaths) throws ChangeSourceException {
        try {
            GitWorkingCopyManager.getInstance().collectWorkingCopyChanges(relevantPaths);
        } catch (final IOException | GitAPIException e) {
            throw new ChangeSourceException(this, e);
        }
    }

    @Override
    public File determineWorkingCopyRoot(final File projectRoot) throws ChangeSourceException {
        try {
            return new FileRepositoryBuilder().findGitDir(projectRoot).build().getDirectory().getParentFile();
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
