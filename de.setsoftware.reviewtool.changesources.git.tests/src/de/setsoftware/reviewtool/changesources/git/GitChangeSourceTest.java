package de.setsoftware.reviewtool.changesources.git;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.OperationCanceledException;
import org.junit.Before;
import org.junit.Test;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.model.api.FileChangeType;
import de.setsoftware.reviewtool.model.api.IBinaryChange;
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.ITextualChange;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Tests for {@link GitChangeSource}.
 */
public class GitChangeSourceTest {

    private static final class ChangeSourceUiStub implements IChangeSourceUi {

        private final StringBuilder log = new StringBuilder();
        private boolean canceled;
        private final Boolean questionResult;

        public ChangeSourceUiStub(Boolean b) {
            this.questionResult = b;
        }

        @Override
        public void beginTask(String name, int totalWork) {
            this.log.append("beginTask ").append(name).append(' ').append(totalWork).append('\n');
        }

        @Override
        public void done() {
            this.log.append("done\n");
        }

        @Override
        public void internalWorked(double work) {
            this.log.append("internalWorked ").append(Math.round(work *  1000)).append('\n');
        }

        @Override
        public boolean isCanceled() {
            return this.canceled;
        }

        @Override
        public void setCanceled(boolean value) {
            this.canceled = value;
        }

        @Override
        public void setTaskName(String name) {
            this.log.append("setTaskName ").append(name).append('\n');
        }

        @Override
        public void subTask(String name) {
            this.log.append("subTask ").append(name).append('\n');
        }

        @Override
        public void worked(int work) {
            this.log.append("worked ").append(work).append('\n');
        }

        @Override
        public Boolean handleLocalWorkingIncomplete(String detailInfo) {
            this.log.append("asking ").append(detailInfo).append('\n');
            return this.questionResult;
        }

        @Override
        public void increaseTaskNestingLevel() {
            this.log.append("increaseTaskNestingLevel\n");
        }

        @Override
        public void decreaseTaskNestingLevel() {
            this.log.append("decreaseTaskNestingLevel\n");
        }

        public String getLog() {
            return this.log.toString();
        }

    }

    private static GitChangeSource createCs() {
        return new GitChangeSource(".*${key}[^0-9].*", 1000000);
    }

    private static GitChangeSource createCs(TestdataRepo repo) throws Exception {
        final GitChangeSource src = createCs();
        src.addProject(repo.getGitBaseDir());
        src.clearCaches();
        return src;
    }

    private static ChangeSourceUiStub createUi() {
        return new ChangeSourceUiStub(null);
    }

    private static ChangeSourceUiStub createUi(Boolean questionResult) {
        return new ChangeSourceUiStub(questionResult);
    }

    private static ITextualChange textualChange(IWorkingCopy wc,
            String oldRevision, int oldRevisionTime,
            String newRevision, int newRevisionTime,
            String path, int lineFrom, int lineTo) {

        final IRevisionedFile oldFile = ChangestructureFactory.createFileInRevision(path,
                createRevision(wc, oldRevision, oldRevisionTime));
        final IRevisionedFile newFile = ChangestructureFactory.createFileInRevision(path,
                createRevision(wc, newRevision, newRevisionTime));
        final IPositionInText from = ChangestructureFactory.createPositionInText(lineFrom, 1);
        final IPositionInText to = ChangestructureFactory.createPositionInText(lineTo, 1);
        return ChangestructureFactory.createTextualChangeHunk(wc, FileChangeType.OTHER,
                ChangestructureFactory.createFragment(oldFile, from, to),
                ChangestructureFactory.createFragment(newFile, from, to));
    }

    private static IBinaryChange binaryChange(IWorkingCopy wc,
            FileChangeType changeType,
            String oldRevision, int oldRevisionTime,
            String newRevision, int newRevisionTime,
            String path) {

        return binaryChange(wc, changeType, oldRevision, oldRevisionTime, newRevision, newRevisionTime, path, path);
    }

    private static IBinaryChange binaryChange(IWorkingCopy wc,
            FileChangeType changeType,
            String oldRevision, int oldRevisionTime,
            String newRevision, int newRevisionTime,
            String oldPath, String newPath) {

        final IRevisionedFile oldFile = ChangestructureFactory.createFileInRevision(oldPath,
                createRevision(wc, oldRevision, oldRevisionTime));
        final IRevisionedFile newFile = ChangestructureFactory.createFileInRevision(newPath,
                createRevision(wc, newRevision, newRevisionTime));
        return ChangestructureFactory.createBinaryChange(wc, changeType, oldFile, newFile);
    }

    private static IRevision createRevision(IWorkingCopy wc, String revision, int revisionTime) {
        if (revision == null) {
            return ChangestructureFactory.createUnknownRevision(wc.getRepository());
        }
        return ChangestructureFactory.createRepoRevision(new RevisionId(revision, revisionTime), wc.getRepository());
    }

    @Before
    public void setUp() {
        GitWorkingCopyManager.reset();
        Logger.setLogger(new Logger() {
            @Override
            protected void log(int status, String message, Throwable exception) {
            }
            @Override
            protected void log(int status, String message) {
            }
        });
    }

    @Test
    public void testNoProjects() throws Exception {
        final GitChangeSource src = createCs();
        final IChangeSourceUi ui = createUi();

        final IChangeData actual = src.getRepositoryChanges("TIC-123", ui);
        assertThat(actual.getRepositories(), is(equalTo(Collections.emptySet())));
        assertThat(actual.getMatchedCommits(), is(equalTo(Collections.emptyList())));
    }

    @Test
    public void testSimpleChange() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addFile("A", 10).commit("TIC-1: Initial commit");
            repo.change("A", 3, "new line content")
                .change("A", 4, "new line content")
                .commit("TIC-2: Another commit");
            final long commitTime = 17000;

            final GitChangeSource src = createCs(repo);
            final ChangeSourceUiStub ui = createUi();

            final IChangeData actual1 = src.getRepositoryChanges("TIC-2", ui);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    textualChange(commits.get(0).getWorkingCopy(),
                            repo.mapToHash("commit 1"), 15,
                            repo.mapToHash("commit 2"), 18,
                            "A", 4, 6),
                    changes.get(0));
            assertEquals(1, changes.size());
            assertEquals(1, commits.size());

            final IChangeData actual2 = src.getRepositoryChanges("TIC-3", ui);
            assertThat(actual2.getMatchedCommits(), is(equalTo(Collections.emptyList())));

            assertEquals(
                    "subTask Determining relevant commits...\n" +
                    "increaseTaskNestingLevel\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 2") + "\n" +
                    "decreaseTaskNestingLevel\n" +
                    "subTask Analyzing commits...\n" +
                    "subTask Determining relevant commits...\n" +
                    "increaseTaskNestingLevel\n" +
                    "decreaseTaskNestingLevel\n" +
                    "subTask Analyzing commits...\n",
                    ui.getLog());
        } finally {
            repo.clean();
        }
    }

    @Test
    public void testBinaryChange() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("B").commit("TIC-1: Initial commit");
            repo.changeBinaryFile("B")
                .commit("TIC-2: Another commit");
            final long commitTime = 17000;

            final GitChangeSource src = createCs(repo);
            final IChangeSourceUi ui = createUi();

            final IChangeData actual1 = src.getRepositoryChanges("TIC-2", ui);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            checkContainsOneBinaryChange(repo, commits.get(0), "commit 1", 15, "commit 2", 18, FileChangeType.OTHER, "B");
            assertEquals(1, commits.size());
        } finally {
            repo.clean();
        }
    }

    @Test
    public void testAddFile() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("B").commit("TIC-1: Initial commit");
            repo.addBinaryFile("C")
                .commit("TIC-2: Another commit");
            final long commitTime = 17000;

            final GitChangeSource src = createCs(repo);
            final IChangeSourceUi ui = createUi();

            final IChangeData actual1 = src.getRepositoryChanges("TIC-2", ui);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            checkContainsOneBinaryChange(repo, commits.get(0), null, 15, "commit 2", 18, FileChangeType.ADDED, "C");
            assertEquals(1, commits.size());
        } finally {
            repo.clean();
        }
    }

    @Test
    public void testDeleteFile() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("B").commit("TIC-1: Initial commit");
            repo.deleteFile("B")
                .commit("TIC-2: Another commit");
            final long commitTime = 17000;

            final GitChangeSource src = createCs(repo);
            final IChangeSourceUi ui = createUi();

            final IChangeData actual1 = src.getRepositoryChanges("TIC-2", ui);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            checkContainsOneBinaryChange(repo, commits.get(0), "commit 1", 15, "commit 2", 18, FileChangeType.DELETED, "B");
            assertEquals(1, commits.size());
        } finally {
            repo.clean();
        }
    }

    @Test
    public void testRenameFile() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("B").commit("TIC-1: Initial commit");
            repo.renameFile("B", "C")
                .commit("TIC-2: Another commit");
            final long commitTime = 17000;

            final GitChangeSource src = createCs(repo);
            final IChangeSourceUi ui = createUi();

            final IChangeData actual1 = src.getRepositoryChanges("TIC-2", ui);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    binaryChange(commits.get(0).getWorkingCopy(),
                            FileChangeType.OTHER,
                            repo.mapToHash("commit 1"), 15,
                            repo.mapToHash("commit 2"), 18,
                            "B", "C"),
                    changes.get(0));
            assertEquals(1, changes.size());
            assertEquals(1, commits.size());
        } finally {
            repo.clean();
        }
    }

    @Test
    public void testCopyFile() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("B").commit("TIC-1: Initial commit");
            repo.copyFile("B", "C")
                .commit("TIC-2: Another commit");
            final long commitTime = 17000;

            final GitChangeSource src = createCs(repo);
            final IChangeSourceUi ui = createUi();

            final IChangeData actual1 = src.getRepositoryChanges("TIC-2", ui);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    binaryChange(commits.get(0).getWorkingCopy(),
                            FileChangeType.OTHER,
                            repo.mapToHash("commit 1"), 15,
                            repo.mapToHash("commit 2"), 18,
                            "B", "C"),
                    changes.get(0));
            assertEquals(1, changes.size());
            assertEquals(1, commits.size());
        } finally {
            repo.clean();
        }
    }

    @Test
    public void testMultipleCommits() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("B").commit("TIC-1: Initial commit");
            repo.changeBinaryFile("B").commit("TIC-1: Commit 2");
            repo.changeBinaryFile("B").commit("TIC-1: Commit 3");
            repo.changeBinaryFile("B").commit("TIC-1: Commit 4");

            final GitChangeSource src = createCs(repo);
            final IChangeSourceUi ui = createUi();

            final IChangeData actual1 = src.getRepositoryChanges("TIC-1", ui);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();

            assertEquals("TIC-1: Initial commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 1") + ")", commits.get(0).getMessage());
            assertEquals(new Date(14000), commits.get(0).getTime());
            checkContainsOneBinaryChange(repo, commits.get(0), null, 11, "commit 1", 15, FileChangeType.ADDED, "B");

            assertEquals("TIC-1: Commit 2 (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(1).getMessage());
            assertEquals(new Date(17000), commits.get(1).getTime());
            checkContainsOneBinaryChange(repo, commits.get(1), "commit 1", 15, "commit 2", 18, FileChangeType.OTHER, "B");

            assertEquals("TIC-1: Commit 3 (1970-01-01 01:00, author, " + repo.mapToHash("commit 3") + ")", commits.get(2).getMessage());
            assertEquals(new Date(20000), commits.get(2).getTime());
            checkContainsOneBinaryChange(repo, commits.get(2), "commit 2", 18, "commit 3", 21, FileChangeType.OTHER, "B");

            assertEquals("TIC-1: Commit 4 (1970-01-01 01:00, author, " + repo.mapToHash("commit 4") + ")", commits.get(3).getMessage());
            assertEquals(new Date(23000), commits.get(3).getTime());
            checkContainsOneBinaryChange(repo, commits.get(3), "commit 3", 21, "commit 4", 24, FileChangeType.OTHER, "B");

            assertEquals(4, commits.size());
        } finally {
            repo.clean();
        }
    }

    private static void checkContainsOneBinaryChange(final TestdataRepo repo, ICommit commit,
            String fromCommit, int fromTime, String toCommit, int toTime,
            FileChangeType other, String filename) {
        final List<? extends IChange> changes2 = commit.getChanges();
        assertEquals(
                binaryChange(commit.getWorkingCopy(),
                        other,
                        repo.mapToHash(fromCommit), fromTime,
                        repo.mapToHash(toCommit), toTime,
                        filename),
                changes2.get(0));
        assertEquals(1, changes2.size());
    }

    @Test
    public void testDetermineLocalChanges() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addFile("A", 10).commit("TIC-1: Initial commit");
            repo.change("A", 3, "new line content")
                .change("A", 4, "new line content");

            final GitChangeSource src = createCs(repo);
            src.analyzeLocalChanges(null);

            final GitWorkingCopy workingCopy =
                    GitWorkingCopyManager.getInstance().getWorkingCopy(repo.getGitBaseDir());

            final List<IRevisionedFile> latestFiles = workingCopy.getFileHistoryGraph().getLatestFiles(
                    ChangestructureFactory.createFileInRevision("A",
                            ChangestructureFactory.createRepoRevision(
                                    new RevisionId(repo.mapToHash("commit 1"), 15),
                                    workingCopy.getRepository())),
                    false);

            assertEquals("A", latestFiles.get(0).getPath());
            assertEquals(ChangestructureFactory.createLocalRevision(workingCopy), latestFiles.get(0).getRevision());
            assertEquals(
                    "line 0\n" +
                    "line 1\n" +
                    "line 2\n" +
                    "new line content\n" +
                    "new line content\n" +
                    "line 5\n" +
                    "line 6\n" +
                    "line 7\n" +
                    "line 8\n" +
                    "line 9\n",
                    new String(latestFiles.get(0).getContents(), "UTF-8"));
            assertEquals(1, latestFiles.size());

        } finally {
            repo.clean();
        }
    }


    @Test
    public void testMultipleBranches() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("A").commit("TIC-1: Initial commit");
            repo.addBinaryFile("B").commit("TIC-2: Commit in another ticket");
            repo.changeBinaryFile("A").commit("TIC-1: commit 3");
            repo.changeBinaryFile("A").commit("TIC-1: commit 4");
            repo.createAndSwitchBranch(repo.mapToHash("commit 2"), "myBranch");
            repo.changeBinaryFile("A").commit("TIC-1: commit 5 (in another branch)");

            final GitChangeSource src = createCs(repo);

            //user chooses to include all commits
            final ChangeSourceUiStub uiWithAll = createUi(Boolean.FALSE);
            final IChangeData actualWithAll = src.getRepositoryChanges("TIC-1", uiWithAll);
            final List<? extends ICommit> commitsWithAll = actualWithAll.getMatchedCommits();
            assertEquals("TIC-1: Initial commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 1") + ")", commitsWithAll.get(0).getMessage());
            assertEquals(new Date(14000), commitsWithAll.get(0).getTime());
            checkContainsOneBinaryChange(repo, commitsWithAll.get(0), null, 11, "commit 1", 15, FileChangeType.ADDED, "A");
            assertEquals("TIC-1: commit 3 (1970-01-01 01:00, author, " + repo.mapToHash("commit 3") + ")", commitsWithAll.get(1).getMessage());
            assertEquals(new Date(20000), commitsWithAll.get(1).getTime());
            checkContainsOneBinaryChange(repo, commitsWithAll.get(1), "commit 2", 18, "commit 3", 21, FileChangeType.OTHER, "A");
            assertEquals("TIC-1: commit 4 (1970-01-01 01:00, author, " + repo.mapToHash("commit 4") + ")", commitsWithAll.get(2).getMessage());
            assertEquals(new Date(23000), commitsWithAll.get(2).getTime());
            checkContainsOneBinaryChange(repo, commitsWithAll.get(2), "commit 3", 21, "commit 4", 24, FileChangeType.OTHER, "A");
            assertEquals("TIC-1: commit 5 (in another branch) (1970-01-01 01:00, author, " + repo.mapToHash("commit 5") + ")", commitsWithAll.get(3).getMessage());
            assertEquals(new Date(28000), commitsWithAll.get(3).getTime());
            checkContainsOneBinaryChange(repo, commitsWithAll.get(3), "commit 2", 18, "commit 5", 29, FileChangeType.OTHER, "A");
            assertEquals(4, commitsWithAll.size());
            assertEquals(
                    "subTask Determining relevant commits...\n" +
                    "increaseTaskNestingLevel\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 5") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 2") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 1") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 4") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 3") + "\n" +
                    "decreaseTaskNestingLevel\n" +
                    "asking The current HEAD does not contain all commits for the ticket (other refs: [refs/heads/master]). Restrict review to current HEAD?\n" +
                    "subTask Analyzing commits...\n",
                    uiWithAll.getLog());

            //user chooses to include only the current branch
            final ChangeSourceUiStub uiOnlyHead = createUi(Boolean.TRUE);
            final IChangeData actualOnlyHead = src.getRepositoryChanges("TIC-1", uiOnlyHead);
            final List<? extends ICommit> commitsOnlyHead = actualOnlyHead.getMatchedCommits();
            assertEquals("TIC-1: Initial commit (1970-01-01 01:00, author, " + repo.mapToHash("commit 1") + ")", commitsOnlyHead.get(0).getMessage());
            assertEquals(new Date(14000), commitsOnlyHead.get(0).getTime());
            checkContainsOneBinaryChange(repo, commitsOnlyHead.get(0), null, 11, "commit 1", 15, FileChangeType.ADDED, "A");
            assertEquals("TIC-1: commit 5 (in another branch) (1970-01-01 01:00, author, " + repo.mapToHash("commit 5") + ")", commitsOnlyHead.get(1).getMessage());
            assertEquals(new Date(28000), commitsOnlyHead.get(1).getTime());
            checkContainsOneBinaryChange(repo, commitsOnlyHead.get(1), "commit 2", 18, "commit 5", 29, FileChangeType.OTHER, "A");
            assertEquals(2, commitsOnlyHead.size());
            assertEquals(
                    "subTask Determining relevant commits...\n" +
                    "increaseTaskNestingLevel\n" +
                    "decreaseTaskNestingLevel\n" +
                    "asking The current HEAD does not contain all commits for the ticket (other refs: [refs/heads/master]). Restrict review to current HEAD?\n" +
                    "subTask Analyzing commits...\n",
                    uiOnlyHead.getLog());


            //user chooses to cancel
            final ChangeSourceUiStub uiCancel = createUi(null);
            try {
                src.getRepositoryChanges("TIC-1", uiCancel);
                fail("expected exception");
            } catch (final OperationCanceledException e) {
            }
        } finally {
            repo.clean();
        }
    }

    @Test
    public void testBranchAndMerge() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("A")
                .addBinaryFile("B")
                .addBinaryFile("C")
                .commit("TIC-1: Initial commit");
            repo.changeBinaryFile("A").commit("TIC-2: commit 2");
            repo.changeBinaryFile("B").commit("TIC-2: commit 3");
            repo.createAndSwitchBranch(repo.mapToHash("commit 2"), "myBranch");
            repo.changeBinaryFile("C").commit("TIC-2: commit 4 (in another branch)");
            repo.merge("myBranch", "master");
            repo.changeBinaryFile("A").commit("TIC-2: commit 6");

            final GitChangeSource src = createCs(repo);
            final ChangeSourceUiStub ui = createUi();

            final IChangeData actual = src.getRepositoryChanges("TIC-2", ui);
            final List<? extends ICommit> commits = actual.getMatchedCommits();

            assertEquals("TIC-2: commit 2 (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(0).getMessage());
            assertEquals(new Date(17000), commits.get(0).getTime());
            checkContainsOneBinaryChange(repo, commits.get(0), "commit 1", 15, "commit 2", 18, FileChangeType.OTHER, "A");

            assertEquals("TIC-2: commit 3 (1970-01-01 01:00, author, " + repo.mapToHash("commit 3") + ")", commits.get(1).getMessage());
            assertEquals(new Date(20000), commits.get(1).getTime());
            checkContainsOneBinaryChange(repo, commits.get(1), "commit 2", 18, "commit 3", 21, FileChangeType.OTHER, "B");

            assertEquals("TIC-2: commit 4 (in another branch) (1970-01-01 01:00, author, " + repo.mapToHash("commit 4") + ")", commits.get(2).getMessage());
            assertEquals(new Date(25000), commits.get(2).getTime());
            checkContainsOneBinaryChange(repo, commits.get(2), "commit 2", 18, "commit 4", 26, FileChangeType.OTHER, "C");

            assertEquals("TIC-2: commit 6 (1970-01-01 01:00, author, " + repo.mapToHash("commit 6") + ")", commits.get(3).getMessage());
            assertEquals(new Date(32000), commits.get(3).getTime());
            checkContainsOneBinaryChange(repo, commits.get(3), "commit 5", 30, "commit 6", 33, FileChangeType.OTHER, "A");

            assertEquals(4, commits.size());
            assertEquals(
                    "subTask Determining relevant commits...\n" +
                    "increaseTaskNestingLevel\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 6") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 5") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 4") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 3") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 2") + "\n" +
                    "decreaseTaskNestingLevel\n" +
                    "subTask Analyzing commits...\n",
                    ui.getLog());

        } finally {
            repo.clean();
        }
    }

    @Test
    public void testBranchAndMerge2() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            repo.addBinaryFile("A").commit("TIC-1: Initial commit");
            repo.addBinaryFile("B").commit("TIC-2: commit 2");
            repo.createAndSwitchBranch(repo.mapToHash("commit 1"), "myBranch");
            repo.addBinaryFile("C").commit("TIC-2: commit 3 (in another branch)");
            repo.merge("myBranch", "master");
            repo.changeBinaryFile("A").commit("TIC-2: commit 5");
            repo.changeBinaryFile("B").commit("TIC-2: commit 6");
            repo.changeBinaryFile("C").commit("TIC-2: commit 7");

            final GitChangeSource src = createCs(repo);
            final ChangeSourceUiStub ui = createUi();

            final IChangeData actual = src.getRepositoryChanges("TIC-2", ui);
            final List<? extends ICommit> commits = actual.getMatchedCommits();

            assertEquals("TIC-2: commit 2 (1970-01-01 01:00, author, " + repo.mapToHash("commit 2") + ")", commits.get(0).getMessage());
            assertEquals(new Date(17000), commits.get(0).getTime());
            checkContainsOneBinaryChange(repo, commits.get(0), null, 15, "commit 2", 18, FileChangeType.ADDED, "B");

            assertEquals("TIC-2: commit 3 (in another branch) (1970-01-01 01:00, author, " + repo.mapToHash("commit 3") + ")", commits.get(1).getMessage());
            assertEquals(new Date(22000), commits.get(1).getTime());
            checkContainsOneBinaryChange(repo, commits.get(1), null, 15, "commit 3", 23, FileChangeType.ADDED, "C");

            assertEquals("TIC-2: commit 5 (1970-01-01 01:00, author, " + repo.mapToHash("commit 5") + ")", commits.get(2).getMessage());
            assertEquals(new Date(29000), commits.get(2).getTime());
            checkContainsOneBinaryChange(repo, commits.get(2), "commit 4", 27, "commit 5", 30, FileChangeType.OTHER, "A");

            assertEquals("TIC-2: commit 6 (1970-01-01 01:00, author, " + repo.mapToHash("commit 6") + ")", commits.get(3).getMessage());
            assertEquals(new Date(32000), commits.get(3).getTime());
            checkContainsOneBinaryChange(repo, commits.get(3), "commit 5", 30, "commit 6", 33, FileChangeType.OTHER, "B");

            assertEquals("TIC-2: commit 7 (1970-01-01 01:00, author, " + repo.mapToHash("commit 7") + ")", commits.get(4).getMessage());
            assertEquals(new Date(35000), commits.get(4).getTime());
            checkContainsOneBinaryChange(repo, commits.get(4), "commit 6", 33, "commit 7", 36, FileChangeType.OTHER, "C");

            assertEquals(5, commits.size());
            assertEquals(
                    "subTask Determining relevant commits...\n" +
                    "increaseTaskNestingLevel\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 7") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 6") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 5") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 4") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 3") + "\n" +
                    "subTask Processing revision " + repo.mapToHash("commit 2") + "\n" +
                    "decreaseTaskNestingLevel\n" +
                    "subTask Analyzing commits...\n",
                    ui.getLog());

        } finally {
            repo.clean();
        }
    }

}
