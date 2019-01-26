package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import de.setsoftware.reviewtool.model.api.AbstractChangeSource;
import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
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
    public IChangeData getRepositoryChanges(final String key, final IChangeSourceUi ui) {
        // TODO implement getRepositoryChanges()
        return ChangestructureFactory.createChangeData(this, Collections.emptyList());
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
