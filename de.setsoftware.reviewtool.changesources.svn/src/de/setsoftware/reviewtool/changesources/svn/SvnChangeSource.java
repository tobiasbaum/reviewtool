package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import de.setsoftware.reviewtool.base.Pair;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.diffalgorithms.IDiffAlgorithm;
import de.setsoftware.reviewtool.model.changestructure.Change;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.Commit;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.IChangeData;
import de.setsoftware.reviewtool.model.changestructure.IChangeSource;
import de.setsoftware.reviewtool.model.changestructure.IChangeSourceUi;
import de.setsoftware.reviewtool.model.changestructure.RepoRevision;

/**
 * A simple change source that loads the changes from subversion.
 */
public class SvnChangeSource implements IChangeSource {

    private static final String KEY_PLACEHOLDER = "${key}";

    private final Set<File> workingCopyRoots;
    private final String logMessagePattern;
    private final SVNClientManager mgr = SVNClientManager.newInstance();
    private final long maxTextDiffThreshold;

    public SvnChangeSource(
            List<File> projectRoots,
            String logMessagePattern,
            String user,
            String pwd,
            long maxTextDiffThreshold) {
        this.mgr.setAuthenticationManager(new DefaultSVNAuthenticationManager(
                null, false, user, pwd.toCharArray(), null, null));
        this.workingCopyRoots = this.determineWorkingCopyRoots(projectRoots);

        this.logMessagePattern = logMessagePattern;
        //check that the pattern can be parsed
        this.createPatternForKey("TEST-123");
        this.maxTextDiffThreshold = maxTextDiffThreshold;
    }

    private Set<File> determineWorkingCopyRoots(List<File> projectRoots) {
        final LinkedHashSet<File> workingCopyRoots = new LinkedHashSet<>();
        for (final File projectRoot : projectRoots) {
            final File wcRoot = this.determineWorkingCopyRoot(projectRoot);
            if (wcRoot != null) {
                workingCopyRoots.add(wcRoot);
            }
        }
        return workingCopyRoots;
    }

    private File determineWorkingCopyRoot(File projectRoot) {
        File curPotentialRoot = projectRoot;
        while (!this.isPotentialRoot(curPotentialRoot)) {
            curPotentialRoot = curPotentialRoot.getParentFile();
            if (curPotentialRoot == null) {
                return null;
            }
        }
        while (true) {
            final File next = curPotentialRoot.getParentFile();
            if (next == null || !this.isPotentialRoot(next)) {
                return curPotentialRoot;
            }
            curPotentialRoot = next;
        }
    }

    private boolean isPotentialRoot(File next) {
        final File dotsvn = new File(next, ".svn");
        return dotsvn.isDirectory();
    }

    private Pattern createPatternForKey(String key) {
        return Pattern.compile(
                this.logMessagePattern.replace(KEY_PLACEHOLDER, Pattern.quote(key)),
                Pattern.DOTALL);
    }

    @Override
    public IChangeData getChanges(String key, IChangeSourceUi ui) {
        try {
            final FileHistoryGraph historyGraph = new FileHistoryGraph();
            final List<SvnRevision> revisions = this.determineRelevantRevisions(key, historyGraph);
            this.checkWorkingCopiesUpToDate(revisions, ui);
            return new SvnChangeData(this.convertToChanges(revisions), historyGraph);
        } catch (final SVNException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    private void checkWorkingCopiesUpToDate(
            List<SvnRevision> revisions,
            IChangeSourceUi ui) throws SVNException {

        final Map<SvnRepo, Long> neededRevisionPerRepo = this.determineMaxRevisionPerRepo(revisions);
        for (final Entry<SvnRepo, Long> e : neededRevisionPerRepo.entrySet()) {
            final File wc = e.getKey().getLocalRoot();
            final long wcRev = this.mgr.getStatusClient().doStatus(wc, false).getRevision().getNumber();
            if (wcRev < e.getValue()) {
                final boolean doUpdate = ui.handleLocalWorkingCopyOutOfDate(wc.toString());
                if (doUpdate) {
                    this.mgr.getUpdateClient().doUpdate(wc, SVNRevision.HEAD, SVNDepth.INFINITY, true, false);
                }
            }
        }
    }

    private Map<SvnRepo, Long> determineMaxRevisionPerRepo(
            List<SvnRevision> revisions) {
        final Map<SvnRepo, Long> ret = new LinkedHashMap<>();
        for (final SvnRevision p : revisions) {
            final SvnRepo repo = p.getRepository();
            final long curRev = p.getRevision();
            if (ret.containsKey(repo)) {
                if (curRev > ret.get(repo)) {
                    ret.put(repo, curRev);
                }
            } else {
                ret.put(repo, curRev);
            }

        }
        return ret;
    }

    private List<SvnRevision> determineRelevantRevisions(
            final String key, FileHistoryGraph historyGraphBuffer) throws SVNException {
        final RelevantRevisionLookupHandler handler = new RelevantRevisionLookupHandler(this.createPatternForKey(key));
        for (final File workingCopyRoot : this.workingCopyRoots) {
            CachedLog.getInstance().traverseRecentEntries(this.mgr, workingCopyRoot, handler);
        }
        return handler.determineRelevantRevisions(historyGraphBuffer);
    }

    private List<Commit> convertToChanges(List<SvnRevision> revisions)
            throws SVNException, IOException {
        final List<Commit> ret = new ArrayList<>();
        for (final SvnRevision e : revisions) {
            ret.add(this.convertToCommit(e));
        }
        return ret;
    }

    private Commit convertToCommit(SvnRevision e) throws SVNException, IOException {
        return ChangestructureFactory.createCommit(
                String.format("%s (Rev. %s, %s)" + (e.isVisible() ? "" : " [invisible]"),
                        e.getMessage(), e.getRevision(), e.getAuthor()),
                        this.determineChangesInCommit(e), e.isVisible());
    }

    /**
     * Helpers class to account for the fact that SVN does not fill the copy path
     * for single files when the whole containing directory has been copied.
     */
    private static final class DirectoryMoveInfo {
        private final List<Pair<String, String>> directoryCopies = new ArrayList<>();

        public DirectoryMoveInfo(Collection<CachedLogEntryPath> values) {
            for (final CachedLogEntryPath p : values) {
                if (p.isDir() && p.getCopyPath() != null) {
                    this.directoryCopies.add(Pair.create(p.getCopyPath(), p.getPath()));
                }
            }
        }

        private String determineOldPath(CachedLogEntryPath entryInfo) {
            if (entryInfo.getCopyPath() != null) {
                return entryInfo.getCopyPath();
            }
            final String path = entryInfo.getPath();
            for (final Pair<String, String> dirCopy : this.directoryCopies) {
                if (path.startsWith(dirCopy.getSecond())) {
                    return dirCopy.getFirst() + path.substring(dirCopy.getSecond().length());
                }
            }
            return path;
        }

    }

    private List<Change> determineChangesInCommit(SvnRevision e)
            throws SVNException, IOException {
        final List<Change> ret = new ArrayList<>();
        final SVNRevision revision = SVNRevision.create(e.getRevision());
        final Map<String, CachedLogEntryPath> changedPaths = e.getChangedPaths();
        final DirectoryMoveInfo dirMoves = new DirectoryMoveInfo(changedPaths.values());
        final Set<String> moveSources = this.determineMoveSources(changedPaths.values(), dirMoves);
        final List<String> sortedPaths = new ArrayList<>(changedPaths.keySet());
        Collections.sort(sortedPaths);
        for (final String path : sortedPaths) {
            final CachedLogEntryPath value = changedPaths.get(path);
            if (!value.isFile()) {
                continue;
            }
            if (moveSources.contains(value.getPath())) {
                //Moves are contained twice, as a copy and a deletion. The deletion shall not result in a fragment.
                continue;
            }
            if (this.isBinaryFile(e.getRepository(), value, e.getRevision())) {
                ret.add(this.createBinaryChange(revision, value, e.getRepository(), e.isVisible(), dirMoves));
            } else {
                ret.addAll(this.determineChangesInFile(revision, e.getRepository(), value, e.isVisible(), dirMoves));
            }
        }
        return ret;
    }

    private Change createBinaryChange(SVNRevision revision, CachedLogEntryPath entryInfo, SvnRepo repo,
            final boolean isVisible, DirectoryMoveInfo dirMoves) {
        final String oldPath = dirMoves.determineOldPath(entryInfo);
        final FileInRevision oldFileInfo =
                ChangestructureFactory.createFileInRevision(oldPath, this.previousRevision(revision), repo);
        final FileInRevision newFileInfo =
                ChangestructureFactory.createFileInRevision(entryInfo.getPath(), this.revision(revision), repo);
        return ChangestructureFactory.createBinaryChange(oldFileInfo, newFileInfo, false, isVisible);
    }

    private RepoRevision revision(SVNRevision revision) {
        return ChangestructureFactory.createRepoRevision(revision.getNumber());
    }

    private RepoRevision previousRevision(SVNRevision revision) {
        return ChangestructureFactory.createRepoRevision(revision.getNumber() - 1);
    }

    private List<Change> determineChangesInFile(SVNRevision revision, SvnRepo repoUrl, CachedLogEntryPath entryInfo,
            final boolean isVisible, DirectoryMoveInfo dirMoves)
            throws IOException {
        final String oldPath = dirMoves.determineOldPath(entryInfo);
        final byte[] oldFileContent = repoUrl.getFileContents(oldPath, this.previousRevision(revision));
        if (this.contentLooksBinary(oldFileContent) || oldFileContent.length > this.maxTextDiffThreshold) {
            return Collections.singletonList(
                    this.createBinaryChange(revision, entryInfo, repoUrl, isVisible, dirMoves));
        }
        final byte[] newFileContent = repoUrl.getFileContents(entryInfo.getPath(), this.revision(revision));
        if (this.contentLooksBinary(newFileContent) || newFileContent.length > this.maxTextDiffThreshold) {
            return Collections.singletonList(
                    this.createBinaryChange(revision, entryInfo, repoUrl, isVisible, dirMoves));
        }


        final FileInRevision oldFileInfo =
                ChangestructureFactory.createFileInRevision(oldPath, this.previousRevision(revision), repoUrl);
        //in case of deletions, the path is null, but FileInRevision does not allow null paths
        final String newPath = entryInfo.getPath() != null ? entryInfo.getPath() : oldPath;
        final FileInRevision newFileInfo =
                ChangestructureFactory.createFileInRevision(newPath, this.revision(revision), repoUrl);
        final List<Change> ret = new ArrayList<>();
        final IDiffAlgorithm diffAlgorithm = DiffAlgorithmFactory.createDefault();
        final List<Pair<Fragment, Fragment>> changes = diffAlgorithm.determineDiff(
                oldFileInfo,
                oldFileContent,
                newFileInfo,
                newFileContent,
                this.guessEncoding(oldFileContent, newFileContent));
        for (final Pair<Fragment, Fragment> pos : changes) {
            ret.add(ChangestructureFactory.createTextualChangeHunk(pos.getFirst(), pos.getSecond(), false, isVisible));
        }
        return ret;
    }

    private boolean contentLooksBinary(byte[] fileContent) {
        if (fileContent.length == 0) {
            return false;
        }
        final int max = Math.min(128, fileContent.length);
        for (int i = 0; i < max; i++) {
            if (this.isStrangeChar(fileContent[i])) {
                //we only count ASCII control chars as "strange" (to be UTF-8 agnostic), so
                //  a single strange char should suffice to declare a file non-text
                return true;
            }
        }
        return false;
    }

    private boolean isStrangeChar(byte b) {
        return b != '\n' && b != '\r' && b != '\t' && b < 0x20 && b >= 0;
    }

    private String guessEncoding(byte[] oldFileContent, byte[] newFileContent) {
        if (this.isValidUtf8(oldFileContent) && this.isValidUtf8(newFileContent)) {
            return "UTF-8";
        } else {
            return "ISO-8859-1";
        }
    }

    /**
     * Returns true iff the given bytes are syntactically valid UTF-8.
     */
    private boolean isValidUtf8(byte[] content) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content));
            return true;
        } catch (final CharacterCodingException e) {
            return false;
        }
    }

    private boolean isBinaryFile(SvnRepo repoUrl, CachedLogEntryPath path, long revision) throws SVNException {
        final long revisionToUse = path.isDeleted() ? revision - 1 : revision;
        final SVNRepository repo = this.mgr.getRepositoryPool().createRepository(repoUrl.getRemoteUrl(), true);
        final SVNProperties propertyBuffer = new SVNProperties();
        repo.getFile(path.getPath(), revisionToUse, propertyBuffer, null);
        final String mimeType = propertyBuffer.getStringValue(SVNProperty.MIME_TYPE);
        return SVNProperty.isBinaryMimeType(mimeType);
    }

    private Set<String> determineMoveSources(Collection<CachedLogEntryPath> entries, DirectoryMoveInfo dirMoves) {
        final Set<String> ret = new LinkedHashSet<>();

        //determine all copy sources
        for (final CachedLogEntryPath p : entries) {
            final String copyPath = dirMoves.determineOldPath(p);
            if (!copyPath.equals(p.getPath())) {
                ret.add(copyPath);
            }
        }

        //if a copy source was deleted, we consider this a "move", everything else is not a move
        for (final CachedLogEntryPath p : entries) {
            if (!p.isDeleted()) {
                ret.remove(p.getPath());
            }
        }

        return ret;
    }

}
