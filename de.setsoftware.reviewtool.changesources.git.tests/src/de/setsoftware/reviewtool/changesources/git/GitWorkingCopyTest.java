package de.setsoftware.reviewtool.changesources.git;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import de.setsoftware.reviewtool.base.Logger;

public class GitWorkingCopyTest {

    @Before
    public void setUp() {
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
    public void testConvertPath() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            final GitWorkingCopy wc = new GitWorkingCopy(repo.getGitBaseDir(), new File("."));
            final File inWc = wc.toAbsolutePathInWc("/a/b/c");
            final String inRepo = wc.toAbsolutePathInRepo(inWc);
            assertEquals("/a/b/c", inRepo);
        } finally {
            repo.clean();
        }
    }

}
