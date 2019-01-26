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
import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSourceUi;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IPositionInText;
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
            assertEquals(1, commits.size());

            final IChangeData actual2 = src.getRepositoryChanges("TIC-3", ui);
            assertSame(actual2.getSource(), src);
            assertThat(actual2.getMatchedCommits(), is(equalTo(Collections.emptyList())));
        } finally {
            repo.clean();
        }
    }

    private static ITextualChange textualChange(IWorkingCopy wc,
            String oldRevision, int oldRevisionTime,
            String newRevision, int newRevisionTime,
            String path, int lineFrom, int lineTo) {

        final IRevisionedFile oldFile = ChangestructureFactory.createFileInRevision(path,
                ChangestructureFactory.createRepoRevision(
                        new RevisionId(oldRevision, oldRevisionTime), wc.getRepository()));
        final IRevisionedFile newFile = ChangestructureFactory.createFileInRevision(path,
                ChangestructureFactory.createRepoRevision(
                        new RevisionId(newRevision, newRevisionTime), wc.getRepository()));
        final IPositionInText from = ChangestructureFactory.createPositionInText(lineFrom, 1);
        final IPositionInText to = ChangestructureFactory.createPositionInText(lineTo, 1);
        return ChangestructureFactory.createTextualChangeHunk(wc, FileChangeType.OTHER,
                ChangestructureFactory.createFragment(oldFile, from, to),
                ChangestructureFactory.createFragment(newFile, from, to));
    }

}
