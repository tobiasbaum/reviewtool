package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.jgit.util.SystemReader;

public class TestdataRepo {

    private static final SystemReader DEFAULT_SYSTEM_READER = SystemReader.getInstance();

    private final File baseDir;
    private int commitCounter;
    private final Map<String, String> commitMap = new LinkedHashMap<>();

    public TestdataRepo() throws IOException, GitAPIException {
        //stub out git's access to the system
        SystemReader.setInstance(new SystemReader() {

            private final AtomicLong currentTime = new AtomicLong(12345);

            @Override
            public FileBasedConfig openUserConfig(Config arg0, FS arg1) {
                return DEFAULT_SYSTEM_READER.openUserConfig(arg0, arg1);
            }

            @Override
            public FileBasedConfig openSystemConfig(Config arg0, FS arg1) {
                return DEFAULT_SYSTEM_READER.openSystemConfig(arg0, arg1);
            }

            @Override
            public String getenv(String arg0) {
                return DEFAULT_SYSTEM_READER.getenv(arg0);
            }

            @Override
            public String getProperty(String arg0) {
                return DEFAULT_SYSTEM_READER.getProperty(arg0);
            }

            @Override
            public int getTimezone(long arg0) {
                return 0;
            }

            @Override
            public String getHostname() {
                return "horst";
            }

            @Override
            public long getCurrentTime() {
                return this.currentTime.getAndAdd(1000);
            }
        });
        this.baseDir = Files.createTempDirectory("pdptesttemp").toFile();
        Git.init().setDirectory(this.baseDir).call();
        try (Git git = Git.open(this.baseDir)) {
            this.storeCommitId(git.commit().setMessage("Initial empty commit").setAllowEmpty(true).call());
        }
    }

    public File getGitBaseDir() throws IOException, GitAPIException {
        return this.baseDir;
    }

    public void clean() throws IOException {
        FileUtils.delete(this.baseDir, FileUtils.RECURSIVE | FileUtils.RETRY);
    }

    public TestdataRepo addFile(String filename, String content) throws IOException, GitAPIException {
        final byte[] bytes = content.getBytes("UTF-8");
        this.writeAndAddFile(filename, bytes);
        return this;
    }

    public TestdataRepo addFile(String filename, int numberOfLines) throws IOException, GitAPIException {
        final StringBuilder content = new StringBuilder();
        for (int i = 0; i < numberOfLines; i++) {
            content.append("line ").append(i).append('\n');
        }
        this.addFile(filename, content.toString());
        return this;
    }

    public TestdataRepo addBinaryFile(String filename) throws IOException, GitAPIException {
        this.writeAndAddFile(filename,
                        mergeBytes(
                            filename.getBytes("UTF-8"),
                            new byte[] {0x00, 0x01, 0x02, 0x03}));
        return this;
    }

    private static byte[] mergeBytes(byte[] b1, byte[] b2) {
        final byte[] ret = Arrays.copyOf(b1, b1.length + b2.length);
        System.arraycopy(b2, 0, ret, b1.length, b2.length);
        return ret;
    }

    private void writeAndAddFile(String filename, byte[] bytes) throws IOException, GitAPIException {
        final File file = this.resolvePath(filename);
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), bytes);
        this.addFile(filename);
    }

    private void addFile(String filename) throws GitAPIException, NoFilepatternException, IOException {
        try (Git git = Git.open(this.baseDir)) {
            git.add().addFilepattern(filename).call();
        }
    }

    public TestdataRepo addLine(String filename, int beforeLineIndex, String additionalContent) throws IOException {
        final Path path = this.resolvePath(filename).toPath();
        final List<String> lines = Files.readAllLines(path, Charset.forName("UTF-8"));
        lines.add(beforeLineIndex, additionalContent);
        this.writeLinesToFile(path, lines);
        return this;
    }

    public TestdataRepo deleteLine(String filename, int index) throws IOException {
        final Path path = this.resolvePath(filename).toPath();
        final List<String> lines = Files.readAllLines(path, Charset.forName("UTF-8"));
        lines.remove(index);
        this.writeLinesToFile(path, lines);
        return this;
    }

    public TestdataRepo deleteLineRange(String filename, int indexFrom, int indexTo) throws IOException {
        final Path path = this.resolvePath(filename).toPath();
        final List<String> lines = Files.readAllLines(path, Charset.forName("UTF-8"));
        for (int i = indexTo; i >= indexFrom; i--) {
        	lines.remove(i);
        }
        this.writeLinesToFile(path, lines);
        return this;
    }

    public TestdataRepo change(String filename, int lineIndex, String newLineContent) throws IOException {
        final Path path = this.resolvePath(filename).toPath();
        final List<String> lines = Files.readAllLines(path, Charset.forName("UTF-8"));
        lines.set(lineIndex, newLineContent);
        this.writeLinesToFile(path, lines);
        return this;
    }

    private void writeLinesToFile(final Path path, final List<String> lines) throws IOException, UnsupportedEncodingException {
        final StringBuilder content = new StringBuilder();
        for (final String s : lines) {
            content.append(s).append('\n');
        }
        Files.write(path, content.toString().getBytes("UTF-8"));
    }

    public TestdataRepo changeBinaryFile(String filename) throws IOException {
        final Path path = this.resolvePath(filename).toPath();
        final byte[] oldContent = Files.readAllBytes(path);
        final byte[] newContent = mergeBytes(oldContent, new byte[] {0x42});
        Files.write(path, newContent);
        return this;
    }

    public TestdataRepo renameFile(String oldFilename, String newFilename) throws IOException, GitAPIException {
        Files.move(this.resolvePath(oldFilename).toPath(), this.resolvePath(newFilename).toPath());
        this.addFile(newFilename);
        return this;
    }

    public TestdataRepo copyFile(String oldFilename, String newFilename) throws IOException, GitAPIException {
        Files.copy(this.resolvePath(oldFilename).toPath(), this.resolvePath(newFilename).toPath());
        this.addFile(newFilename);
        return this;
    }

    public TestdataRepo deleteFile(String filename) throws IOException, GitAPIException {
        Files.delete(this.resolvePath(filename).toPath());
        return this;
    }

    private File resolvePath(String filename) {
        return new File(this.baseDir, filename);
    }

    public void commit(String commitComment) throws GitAPIException, IOException {
        try (Git git = Git.open(this.baseDir)) {
            this.storeCommitId(git.commit().setMessage(commitComment).setAuthor("author", "author@example.com").setAll(true).call());
        }
    }

    public void createAndSwitchBranch(String commitId, String branchName) throws GitAPIException, IOException {
        try (Git git = Git.open(this.baseDir)) {
            git.branchCreate()
                .setName(branchName)
                .setStartPoint(commitId)
                .call();
            git.checkout()
                .setName(branchName)
                .call();
        }
    }

    public void merge(String fromBranch, String toBranch) throws IOException, GitAPIException {
        try (Git git = Git.open(this.baseDir)) {
            git.checkout()
                .setName(toBranch)
                .call();
            final MergeResult mrg = git.merge()
                .include(git.getRepository().resolve(fromBranch))
                .setFastForward(FastForwardMode.NO_FF)
                .setSquash(false)
                .setCommit(false)
                .call();
            if (mrg.getMergeStatus() == MergeStatus.CONFLICTING) {
                throw new AssertionError("conflict while merging " + mrg);
            }
            this.commit("merge " + fromBranch + " into " + toBranch);
        }
    }

    private void storeCommitId(RevCommit c) {
        this.commitMap.put(c.name(), "commit " + (this.commitCounter++));
    }

    public String mapToHash(String commitName) {
        if (commitName == null) {
            return null;
        }
        for (final Entry<String, String> e : this.commitMap.entrySet()) {
            if (e.getValue().equals(commitName)) {
                return e.getKey();
            }
        }
        throw new AssertionError("unknown: " + commitName);
    }

    public long getUniqueTime() {
        return SystemReader.getInstance().getCurrentTime();
    }

    public Map<String, String> getCommitMap() {
        return this.commitMap;
    }

    public Git createGit() throws IOException {
        return Git.open(this.baseDir);
    }

}
