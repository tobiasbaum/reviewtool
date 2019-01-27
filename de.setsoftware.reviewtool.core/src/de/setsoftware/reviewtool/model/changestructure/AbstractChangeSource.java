package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.setsoftware.reviewtool.model.api.ChangeSourceException;
import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.IFileHistoryEdge;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode.Type;
import de.setsoftware.reviewtool.model.api.IHunk;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Superclass with common behavior for usual change sources.
 */
public abstract class AbstractChangeSource implements IChangeSource {

    private static final String KEY_PLACEHOLDER = "${key}";

    private final Map<File, Set<File>> projectsPerWcMap;
    private final String logMessagePattern;
    private final long maxTextDiffThreshold;

    protected AbstractChangeSource(
            final String logMessagePattern,
            final long maxTextDiffThreshold) {

        this.projectsPerWcMap = new LinkedHashMap<>();
        this.logMessagePattern = logMessagePattern;
        //check that the pattern can be parsed
        this.createPatternForKey("TEST-123");
        this.maxTextDiffThreshold = maxTextDiffThreshold;
    }

    protected Pattern createPatternForKey(final String key) {
        return Pattern.compile(
                this.logMessagePattern.replace(KEY_PLACEHOLDER, Pattern.quote(key)),
                Pattern.DOTALL);
    }

    @Override
    public boolean addProject(final File projectRoot) throws ChangeSourceException {
        final File wcRoot = this.determineWorkingCopyRoot(projectRoot);
        if (wcRoot == null) {
            return false;
        }

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
            this.workingCopyAdded(wcRoot);
        }

        return true;
    }

    protected abstract void workingCopyAdded(File wcRoot);

    @Override
    public void removeProject(final File projectRoot) throws ChangeSourceException {
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
                this.workingCopyRemoved(wcRoot);
            }
        }
    }

    protected abstract void workingCopyRemoved(File wcRoot);

    protected List<? extends IChange> determineChangesInFile(
            final IWorkingCopy wc,
            final IFileHistoryNode node) throws Exception {

        final boolean newFileContentsUseTextualDiff = this.isUseTextualDiff(node.getFile());

        final List<IChange> changes = new ArrayList<>();
        for (final IFileHistoryEdge ancestorEdge : node.getAncestors()) {
            final IFileHistoryNode ancestor = ancestorEdge.getAncestor();

            final boolean oldFileContentsUseTextualDiff = this.isUseTextualDiff(ancestor.getFile());

            if (oldFileContentsUseTextualDiff && newFileContentsUseTextualDiff) {
                final List<? extends IHunk> hunks = ancestorEdge.getDiff().getHunks();
                for (final IHunk hunk : hunks) {
                    changes.add(ChangestructureFactory.createTextualChangeHunk(
                            wc,
                            this.mapChangeType(node.getType()),
                            hunk.getSource(),
                            hunk.getTarget()));
                }
            } else {
                changes.add(this.createBinaryChange(wc, node, ancestor));
            }
        }
        return changes;
    }

    private IBinaryChange createBinaryChange(
            final IWorkingCopy wc,
            final IFileHistoryNode node,
            final IFileHistoryNode ancestor) {

        final IRevisionedFile oldFileInfo = ChangestructureFactory.createFileInRevision(
                ancestor.getFile().getPath(),
                ancestor.getFile().getRevision());

        return ChangestructureFactory.createBinaryChange(
                wc,
                this.mapChangeType(node.getType()),
                oldFileInfo,
                node.getFile());
    }

    private FileChangeType mapChangeType(final Type type) {
        switch (type) {
        case ADDED:
            return FileChangeType.ADDED;
        case DELETED:
            return FileChangeType.DELETED;
        default:
            return FileChangeType.OTHER;
        }
    }

    protected final boolean isUseTextualDiff(final IRevisionedFile file) throws Exception {
        final byte[] newFileContent = file.getContents();
        return !contentLooksBinary(newFileContent) && newFileContent.length <= this.maxTextDiffThreshold;
    }

    private static boolean contentLooksBinary(final byte[] fileContent) {
        if (fileContent.length == 0) {
            return false;
        }
        final int max = Math.min(128, fileContent.length);
        for (int i = 0; i < max; i++) {
            if (isStrangeChar(fileContent[i])) {
                //we only count ASCII control chars as "strange" (to be UTF-8 agnostic), so
                //  a single strange char should suffice to declare a file non-text
                return true;
            }
        }
        return false;
    }

    private static boolean isStrangeChar(final byte b) {
        return b != '\n' && b != '\r' && b != '\t' && b < 0x20 && b >= 0;
    }

}
