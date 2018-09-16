package de.setsoftware.reviewtool.changesources.svn;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Tests for {@link SvnWorkingCopy}.
 */
public class SvnWorkingCopyTest {

    @Test
    public void testToAbsolutePathInRepoOnRootWc() {
        final File currentDirectory = Paths.get("").toAbsolutePath().toFile();
        final SvnWorkingCopy wc = new SvnWorkingCopy(new StubRepo(""), new File(currentDirectory, "a"));
        assertThat(wc.toAbsolutePathInRepo(new File(currentDirectory, "a/b/c")), is(equalTo("/b/c")));
    }

    @Test
    public void testToAbsolutePathInRepoOnTrunkWc() {
        final File currentDirectory = Paths.get("").toAbsolutePath().toFile();
        final SvnWorkingCopy wc = new SvnWorkingCopy(new StubRepo("/trunk"), new File(currentDirectory, "a"));
        assertThat(wc.toAbsolutePathInRepo(new File(currentDirectory, "a/b/c")), is(equalTo("/trunk/b/c")));
    }

    @Test
    public void testToAbsolutePathInWcOnRootRepo() {
        final File currentDirectory = Paths.get("").toAbsolutePath().toFile();
        final SvnWorkingCopy wc = new SvnWorkingCopy(new StubRepo(""), new File(currentDirectory, "a"));
        assertThat(wc.toAbsolutePathInWc("/trunk/b/c"), is(equalTo(new File(currentDirectory, "a/trunk/b/c"))));
    }

    @Test
    public void testToAbsolutePathInWcOnTrunkRepo() {
        final File currentDirectory = Paths.get("").toAbsolutePath().toFile();
        final SvnWorkingCopy wc = new SvnWorkingCopy(new StubRepo("/trunk"), new File(currentDirectory, "a"));
        assertThat(wc.toAbsolutePathInWc("/trunk/b/c"), is(equalTo(new File(currentDirectory, "a/b/c"))));
    }
}
