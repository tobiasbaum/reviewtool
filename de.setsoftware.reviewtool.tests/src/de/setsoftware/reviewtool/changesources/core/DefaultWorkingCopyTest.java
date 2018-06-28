package de.setsoftware.reviewtool.changesources.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

import de.setsoftware.reviewtool.changesources.core.DefaultChangeSource;

/**
 * Tests for {@link SvnWorkingCopy}.
 */
public class DefaultWorkingCopyTest extends AbstractChangeSourceTestCase {

    @Test
    public void testToAbsolutePathInRepoOnRootWc() {
        final File currentDirectory = Paths.get("").toAbsolutePath().toFile();
        final File wcDirectory = new File(currentDirectory, "a");
        final DefaultChangeSource<
                StubChangeItem, StubCommitId, StubCommit, StubRepo, StubLocalChange, StubWorkingCopy,
                StubScmRepositoryBridge, StubScmWorkingCopyBridge> changeSource = changeSource();
        final StubWorkingCopy wc = wc(
                changeSource,
                repo(changeSource, wcDirectory.toString()),
                wcDirectory,
                "");
        assertThat(wc.toAbsolutePathInRepo(new File(currentDirectory, "a/b/c")), is(equalTo("/b/c")));
    }

    @Test
    public void testToAbsolutePathInRepoOnTrunkWc() {
        final File currentDirectory = Paths.get("").toAbsolutePath().toFile();
        final File wcDirectory = new File(currentDirectory, "a");
        final DefaultChangeSource<
                StubChangeItem, StubCommitId, StubCommit, StubRepo, StubLocalChange, StubWorkingCopy,
                StubScmRepositoryBridge, StubScmWorkingCopyBridge> changeSource = changeSource();
        final StubWorkingCopy wc = wc(
                changeSource,
                repo(changeSource, wcDirectory.toString()),
                wcDirectory,
                "/trunk");
        assertThat(wc.toAbsolutePathInRepo(new File(currentDirectory, "a/b/c")), is(equalTo("/trunk/b/c")));
    }
}
