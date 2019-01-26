package de.setsoftware.reviewtool.changesources.git;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class GitWorkingCopyTest {

    @Test
    public void testConvertPath() throws Exception {
        final TestdataRepo repo = new TestdataRepo();
        try {
            final GitWorkingCopy wc = new GitWorkingCopy(repo.getGitBaseDir());
            final File inWc = wc.toAbsolutePathInWc("/a/b/c");
            final String inRepo = wc.toAbsolutePathInRepo(inWc);
            assertEquals("/a/b/c", inRepo);
        } finally {
            repo.clean();
        }
    }

}
