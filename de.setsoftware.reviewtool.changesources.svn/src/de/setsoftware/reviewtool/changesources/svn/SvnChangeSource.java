package de.setsoftware.reviewtool.changesources.svn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
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
import de.setsoftware.reviewtool.model.changestructure.IChangeSource;
import de.setsoftware.reviewtool.model.changestructure.IChangeSourceUi;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;
import de.setsoftware.reviewtool.model.changestructure.RepoRevision;

/**
 * A simple change source that loads the changes from subversion.
 */
public class SvnChangeSource implements IChangeSource {

    private static final String KEY_PLACEHOLDER = "${key}";
    private static final int LOOKUP_LIMIT = 1000;

    private final Set<File> workingCopyRoots;
    private final String logMessagePattern;
    private final SVNClientManager mgr = SVNClientManager.newInstance();

    public SvnChangeSource(
            List<File> projectRoots, String logMessagePattern, String user, String pwd) {
        this.mgr.setAuthenticationManager(new DefaultSVNAuthenticationManager(
                null, false, user, pwd.toCharArray(), null, null));
        this.workingCopyRoots = this.determineWorkingCopyRoots(projectRoots);

        this.logMessagePattern = logMessagePattern;
        //check that the pattern can be parsed
        this.createPatternForKey("TEST-123");
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

    /**
     * Handler that filters log entries with the given pattern.
     */
    private class LookupHandler implements ISVNLogEntryHandler {

        private final Pattern pattern;
        private final List<Pair<SvnRepo, SVNLogEntry>> matchingEntries = new ArrayList<>();
        private SvnRepo currentRoot;

        public LookupHandler(Pattern patternForKey) {
            this.pattern = patternForKey;
        }

        public void setCurrentRepo(SvnRepo repo) {
            this.currentRoot = repo;
        }

        @Override
        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
            if (logEntry.getMessage() != null && this.pattern.matcher(logEntry.getMessage()).matches()) {
                assert this.currentRoot != null;
                this.matchingEntries.add(Pair.create(this.currentRoot, logEntry));
            }
        }

    }

    @Override
    public List<Commit> getChanges(String key, IChangeSourceUi ui) {
        try {
            final List<Pair<SvnRepo, SVNLogEntry>> revisions = this.determineRelevantRevisions(key);
            this.sortByDate(revisions);
            this.checkWorkingCopiesUpToDate(revisions, ui);
            return this.convertToChanges(revisions);
        } catch (final SVNException | IOException e) {
            throw new ReviewtoolException(e);
        }
    }

    private void checkWorkingCopiesUpToDate(
            List<Pair<SvnRepo, SVNLogEntry>> revisions,
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
            List<Pair<SvnRepo, SVNLogEntry>> revisions) {
        final Map<SvnRepo, Long> ret = new LinkedHashMap<>();
        for (final Pair<SvnRepo, SVNLogEntry> p : revisions) {
            final SvnRepo repo = p.getFirst();
            final long curRev = p.getSecond().getRevision();
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

    private List<Pair<SvnRepo, SVNLogEntry>> determineRelevantRevisions(String key) throws SVNException {
        final LookupHandler handler = new LookupHandler(this.createPatternForKey(key));
        for (final File workingCopyRoot : this.workingCopyRoots) {
            final SVNURL rootUrl = this.mgr.getLogClient().getReposRoot(workingCopyRoot, null, SVNRevision.HEAD);
            handler.setCurrentRepo(new SvnRepo(
                    workingCopyRoot,
                    rootUrl,
                    this.determineCheckoutPrefix(workingCopyRoot, rootUrl)));

            final SVNURL wcUrl = this.mgr.getWCClient().doInfo(workingCopyRoot, SVNRevision.WORKING).getURL();
            final String relPath = wcUrl.toString().substring(rootUrl.toString().length());
            this.mgr.getLogClient().doLog(
                    rootUrl,
                    new String[] { relPath },
                    SVNRevision.HEAD,
                    SVNRevision.HEAD,
                    SVNRevision.create(0),
                    false,
                    true,
                    false,
                    LOOKUP_LIMIT,
                    new String[0],
                    handler);
        }
        return handler.matchingEntries;
    }

    private int determineCheckoutPrefix(File workingCopyRoot, SVNURL rootUrl) throws SVNException {
        SVNURL checkoutRootUrlPrefix = this.mgr.getWCClient().doInfo(workingCopyRoot, SVNRevision.HEAD).getURL();
        int i = 0;
        while (!(checkoutRootUrlPrefix.equals(rootUrl) || checkoutRootUrlPrefix.getPath().equals("//"))) {
            checkoutRootUrlPrefix = checkoutRootUrlPrefix.removePathTail();
            i++;
        }
        return i;
    }

    private void sortByDate(List<Pair<SvnRepo, SVNLogEntry>> revisions) {
        Collections.sort(revisions, new Comparator<Pair<SvnRepo, SVNLogEntry>>() {
            @Override
            public int compare(Pair<SvnRepo, SVNLogEntry> o1, Pair<SvnRepo, SVNLogEntry> o2) {
                return o1.getSecond().getDate().compareTo(o2.getSecond().getDate());
            }
        });
    }

    private List<Commit> convertToChanges(List<Pair<SvnRepo, SVNLogEntry>> revisions)
            throws SVNException, IOException {
        final List<Commit> ret = new ArrayList<>();
        for (final Pair<SvnRepo, SVNLogEntry> e : revisions) {
            ret.add(this.convertToCommit(e));
        }
        return ret;
    }

    private Commit convertToCommit(Pair<SvnRepo, SVNLogEntry> e) throws SVNException, IOException {
        final SVNLogEntry log = e.getSecond();
        return ChangestructureFactory.createCommit(
                String.format("%s (Rev. %s, %s)", log.getMessage(), log.getRevision(), log.getAuthor()),
                this.determineChangesInCommit(e));
    }

    private List<Change> determineChangesInCommit(Pair<SvnRepo, SVNLogEntry> e)
            throws SVNException, IOException {
        final List<Change> ret = new ArrayList<>();
        final SVNRevision revision = SVNRevision.create(e.getSecond().getRevision());
        final Map<String, SVNLogEntryPath> changedPaths = e.getSecond().getChangedPaths();
        final Set<String> moveSources = this.determineMoveSources(changedPaths.values());
        final List<String> sortedPaths = new ArrayList<>(changedPaths.keySet());
        Collections.sort(sortedPaths);
        for (final String path : sortedPaths) {
            final SVNLogEntryPath value = changedPaths.get(path);
            if (value.getKind() != SVNNodeKind.FILE) {
                continue;
            }
            if (moveSources.contains(value.getPath())) {
                //Moves are contained twice, as a copy and a deletion. The deletion shall not result in a fragment.
                continue;
            }
            if (this.isBinaryFile(e.getFirst(), value, e.getSecond().getRevision())) {
                ret.add(this.createBinaryChange(revision, value, e.getFirst()));
            } else {
                ret.addAll(this.determineChangesInFile(revision, e.getFirst(), value));
            }
        }
        return ret;
    }

    private Change createBinaryChange(SVNRevision revision, SVNLogEntryPath entryInfo, SvnRepo repo) {
        final String oldPath = this.determineOldPath(entryInfo);
        final FileInRevision oldFileInfo =
                ChangestructureFactory.createFileInRevision(oldPath, this.previousRevision(revision), repo);
        final FileInRevision newFileInfo =
                ChangestructureFactory.createFileInRevision(entryInfo.getPath(), this.revision(revision), repo);
        return ChangestructureFactory.createBinaryChange(oldFileInfo, newFileInfo, false);
    }

    private RepoRevision revision(SVNRevision revision) {
        return ChangestructureFactory.createRepoRevision(revision.getNumber());
    }

    private RepoRevision previousRevision(SVNRevision revision) {
        return ChangestructureFactory.createRepoRevision(revision.getNumber() - 1);
    }

    private List<Change> determineChangesInFile(SVNRevision revision, SvnRepo repoUrl, SVNLogEntryPath entryInfo)
            throws SVNException, IOException {
        final String oldPath = this.determineOldPath(entryInfo);
        final byte[] oldFileContent = this.loadFile(repoUrl, oldPath, revision.getNumber() - 1);
        if (this.contentLooksBinary(oldFileContent)) {
            return Collections.singletonList(this.createBinaryChange(revision, entryInfo, repoUrl));
        }
        final byte[] newFileContent = this.loadFile(repoUrl, entryInfo.getPath(), revision.getNumber());
        if (this.contentLooksBinary(newFileContent)) {
            return Collections.singletonList(this.createBinaryChange(revision, entryInfo, repoUrl));
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
            ret.add(ChangestructureFactory.createTextualChangeHunk(pos.getFirst(), pos.getSecond(), false));
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

    private String determineOldPath(SVNLogEntryPath entryInfo) {
        return entryInfo.getCopyPath() == null ? entryInfo.getPath() : entryInfo.getCopyPath();
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

    private boolean isBinaryFile(SvnRepo repoUrl, SVNLogEntryPath path, long revision) throws SVNException {
        final long revisionToUse = path.getType() == 'D' ? revision - 1 : revision;
        final SVNRepository repo = this.mgr.getRepositoryPool().createRepository(repoUrl.getRemoteUrl(), true);
        final SVNProperties propertyBuffer = new SVNProperties();
        repo.getFile(path.getPath(), revisionToUse, propertyBuffer, null);
        final String mimeType = propertyBuffer.getStringValue(SVNProperty.MIME_TYPE);
        return SVNProperty.isBinaryMimeType(mimeType);
    }

    private Set<String> determineMoveSources(Collection<SVNLogEntryPath> entries) {
        final Set<String> ret = new LinkedHashSet<>();

        //determine all copy sources
        for (final SVNLogEntryPath p : entries) {
            if (p.getCopyPath() != null) {
                ret.add(p.getCopyPath());
            }
        }

        //if a copy source was deleted, we consider this a "move", everything else is not a move
        for (final SVNLogEntryPath p : entries) {
            if (p.getType() != 'D') {
                ret.remove(p.getPath());
            }
        }

        return ret;
    }

    private byte[] loadFile(SvnRepo repoUrl, String path, long revision) throws SVNException {
        final SVNRepository repo = this.mgr.getRepositoryPool().createRepository(repoUrl.getRemoteUrl(), true);
        final ByteArrayOutputStream contents = new ByteArrayOutputStream();
        if (repo.checkPath(path, revision) != SVNNodeKind.FILE) {
            return new byte[0];
        }
        repo.getFile(path, revision, null, contents);
        return contents.toByteArray();
    }

    @Override
    public IFragmentTracer createTracer() {
        return new SvnFragmentTracer(this.mgr);
    }

}
