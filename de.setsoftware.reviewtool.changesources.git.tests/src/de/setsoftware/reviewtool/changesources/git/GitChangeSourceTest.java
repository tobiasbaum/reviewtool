package de.setsoftware.reviewtool.changesources.git;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Date;
import java.util.List;

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
        public Boolean handleLocalWorkingCopyOutOfDate(String detailInfo) {
            return Boolean.FALSE;
        }

        @Override
        public void increaseTaskNestingLevel() {
            this.log.append("increaseTaskNestingLevel\n");
        }

        @Override
        public void decreaseTaskNestingLevel() {
            this.log.append("decreaseTaskNestingLevel\n");
        }

    }

    private static GitChangeSource createCs() {
        return new GitChangeSource(".*${key}[^0-9].*", 1000000, 5);
    }

    private static GitChangeSource createCs(TestdataRepo repo) throws Exception {
        final GitChangeSource src = createCs();
        src.addProject(repo.getGitBaseDir());
        return src;
    }

    private static ChangeSourceUiStub createUi() {
        return new ChangeSourceUiStub();
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
        assertSame(actual.getSource(), src);
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
            final IChangeSourceUi ui = createUi();

            final IChangeData actual1 = src.getRepositoryChanges("TIC-2", ui);
            assertSame(actual1.getSource(), src);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, 61c3bff929ddb5f707be87ad066f6e40431b28e8)", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    textualChange(commits.get(0).getWorkingCopy(),
                            "2792782d05508291c0b91d22d6c40ea4756ab6fe", 15,
                            "61c3bff929ddb5f707be87ad066f6e40431b28e8", 18,
                            "A", 4, 6),
                    changes.get(0));
            assertEquals(1, changes.size());
            assertEquals(1, commits.size());

            final IChangeData actual2 = src.getRepositoryChanges("TIC-3", ui);
            assertSame(actual2.getSource(), src);
            assertThat(actual2.getMatchedCommits(), is(equalTo(Collections.emptyList())));
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
            assertSame(actual1.getSource(), src);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, 3b6ecca787d448c2751af45cc09241ad8fb30350)", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    binaryChange(commits.get(0).getWorkingCopy(),
                            FileChangeType.OTHER,
                            "38d01910a5cd338ec7ad86eb3ef4d90f65bb7340", 15,
                            "3b6ecca787d448c2751af45cc09241ad8fb30350", 18,
                            "B"),
                    changes.get(0));
            assertEquals(1, changes.size());
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
            assertSame(actual1.getSource(), src);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, 2de19661733caaf938e72c891aac6e504a9152a2)", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    binaryChange(commits.get(0).getWorkingCopy(),
                            FileChangeType.ADDED,
                            null, 15,
                            "2de19661733caaf938e72c891aac6e504a9152a2", 18,
                            "C"),
                    changes.get(0));
            assertEquals(1, changes.size());
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
            assertSame(actual1.getSource(), src);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, 10c97826165b3cfd2543d155f4cc58611ec7dd93)", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    binaryChange(commits.get(0).getWorkingCopy(),
                            FileChangeType.DELETED,
                            "38d01910a5cd338ec7ad86eb3ef4d90f65bb7340", 15,
                            "10c97826165b3cfd2543d155f4cc58611ec7dd93", 18,
                            "B"),
                    changes.get(0));
            assertEquals(1, changes.size());
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
            assertSame(actual1.getSource(), src);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, 1517599b382a629d4997d48233524cf289f60bd7)", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    binaryChange(commits.get(0).getWorkingCopy(),
                            FileChangeType.OTHER,
                            "38d01910a5cd338ec7ad86eb3ef4d90f65bb7340", 15,
                            "1517599b382a629d4997d48233524cf289f60bd7", 18,
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
            assertSame(actual1.getSource(), src);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();
            assertEquals("TIC-2: Another commit (1970-01-01 01:00, author, 3fca5f263ed7a5dc4698915f8c817188a1a24660)", commits.get(0).getMessage());
            assertEquals(new Date(commitTime), commits.get(0).getTime());
            final List<? extends IChange> changes = commits.get(0).getChanges();
            assertEquals(
                    binaryChange(commits.get(0).getWorkingCopy(),
                            FileChangeType.OTHER,
                            "38d01910a5cd338ec7ad86eb3ef4d90f65bb7340", 15,
                            "3fca5f263ed7a5dc4698915f8c817188a1a24660", 18,
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
            assertSame(actual1.getSource(), src);
            final List<? extends ICommit> commits = actual1.getMatchedCommits();

            assertEquals("TIC-1: Initial commit (1970-01-01 01:00, author, 38d01910a5cd338ec7ad86eb3ef4d90f65bb7340)", commits.get(0).getMessage());
            assertEquals(new Date(14000), commits.get(0).getTime());
            final List<? extends IChange> changes1 = commits.get(0).getChanges();
            assertEquals(
                    binaryChange(commits.get(0).getWorkingCopy(),
                            FileChangeType.ADDED,
                            null, 11,
                            "38d01910a5cd338ec7ad86eb3ef4d90f65bb7340", 15,
                            "B"),
                    changes1.get(0));
            assertEquals(1, changes1.size());

            assertEquals("TIC-1: Commit 2 (1970-01-01 01:00, author, 025ac16a18331c3da1b1f4e2688a139f34d96bd1)", commits.get(1).getMessage());
            assertEquals(new Date(17000), commits.get(1).getTime());
            final List<? extends IChange> changes2 = commits.get(1).getChanges();
            assertEquals(
                    binaryChange(commits.get(1).getWorkingCopy(),
                            FileChangeType.OTHER,
                            "38d01910a5cd338ec7ad86eb3ef4d90f65bb7340", 15,
                            "025ac16a18331c3da1b1f4e2688a139f34d96bd1", 18,
                            "B"),
                    changes2.get(0));
            assertEquals(1, changes2.size());

            assertEquals("TIC-1: Commit 3 (1970-01-01 01:00, author, 90013989c2cda6517a7ad0842406a01fc7e78bdc)", commits.get(2).getMessage());
            assertEquals(new Date(20000), commits.get(2).getTime());
            final List<? extends IChange> changes3 = commits.get(2).getChanges();
            assertEquals(
                    binaryChange(commits.get(2).getWorkingCopy(),
                            FileChangeType.OTHER,
                            "025ac16a18331c3da1b1f4e2688a139f34d96bd1", 18,
                            "90013989c2cda6517a7ad0842406a01fc7e78bdc", 21,
                            "B"),
                    changes3.get(0));
            assertEquals(1, changes3.size());

            assertEquals("TIC-1: Commit 4 (1970-01-01 01:00, author, eb4e7f11cee564527df3ae5ccede34f6ea1b66f3)", commits.get(3).getMessage());
            assertEquals(new Date(23000), commits.get(3).getTime());
            final List<? extends IChange> changes4 = commits.get(3).getChanges();
            assertEquals(
                    binaryChange(commits.get(3).getWorkingCopy(),
                            FileChangeType.OTHER,
                            "90013989c2cda6517a7ad0842406a01fc7e78bdc", 21,
                            "eb4e7f11cee564527df3ae5ccede34f6ea1b66f3", 24,
                            "B"),
                    changes4.get(0));
            assertEquals(1, changes4.size());

            assertEquals(4, commits.size());
        } finally {
            repo.clean();
        }
    }

}
