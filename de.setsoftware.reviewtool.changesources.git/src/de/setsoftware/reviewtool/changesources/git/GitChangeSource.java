package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;

/**
 * A change source that loads the changes from Git.
 */
public class GitChangeSource implements IChangeSource {

    private static final String KEY_PLACEHOLDER = "${key}";

    private final Map<File, Set<File>> projectsPerWcMap;
    private final String logMessagePattern;
    private final long maxTextDiffThreshold;

    /**
     * Constructor.
     */
    GitChangeSource(
            final String logMessagePattern,
            final String user,
            final String pwd,
            final long maxTextDiffThreshold,
            final int logCacheMinSize) {

        this.projectsPerWcMap = new LinkedHashMap<>();
        this.maxTextDiffThreshold = maxTextDiffThreshold;
        this.logMessagePattern = logMessagePattern;

        // check that the pattern can be parsed
        this.createPatternForKey("TEST-123");

        GitRepositoryManager.getInstance().init(logCacheMinSize);
    }

    private Pattern createPatternForKey(final String key) {
        return Pattern.compile(
                this.logMessagePattern.replace(KEY_PLACEHOLDER, Pattern.quote(key)),
                Pattern.DOTALL);
    }

    @Override
    public IChangeData getRepositoryChanges(final String key, final IChangeSourceUi ui) {
        // TODO implement getRepositoryChanges()
        return null;
    }

    @Override
    public void analyzeLocalChanges(List<File> relevantPaths) throws ChangeSourceException {
        // TODO implement analyzeLocalChanges()
    }

    @Override
    public void addProject(File projectRoot) throws ChangeSourceException {

        final File wcRoot = this.determineWorkingCopyRoot(projectRoot);
        if (wcRoot != null) {
            boolean wcCreated = false;
            synchronized (this.projectsPerWcMap) {
                Set<File> projects = this.projectsPerWcMap.get(wcRoot);
                if (projects == null) {
                    projects = new LinkedHashSet<>();
                    this.projectsPerWcMap.put(wcRoot, projects);
                    wcCreated = true;
                }
                projects.add(projectRoot);
            }

            if (wcCreated) {
                GitWorkingCopyManager.getInstance().getWorkingCopy(wcRoot);
            }
        }
    }

    @Override
    public void removeProject(File projectRoot) throws ChangeSourceException {
        final File wcRoot = this.determineWorkingCopyRoot(projectRoot);
        if (wcRoot != null) {
            boolean wcHasProjects = true;
            synchronized (this.projectsPerWcMap) {
                final Set<File> projects = this.projectsPerWcMap.get(wcRoot);
                if (projects != null) {
                    projects.remove(projectRoot);
                    if (projects.isEmpty()) {
                        this.projectsPerWcMap.remove(wcRoot);
                        wcHasProjects = false;
                    }
                }
            }

            if (!wcHasProjects) {
                GitWorkingCopyManager.getInstance().removeWorkingCopy(wcRoot);
            }
        }
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
    public void clearCaches() {
        // TODO Auto-generated method stub

    }
}
