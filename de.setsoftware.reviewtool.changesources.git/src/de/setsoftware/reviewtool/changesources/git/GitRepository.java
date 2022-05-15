package de.setsoftware.reviewtool.changesources.git;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.diffalgorithms.DiffAlgorithmFactory;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.changestructure.AbstractRepository;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryGraph;

/**
 * Wraps the information needed on a Git repository and corresponding working copy.
 */
final class GitRepository extends AbstractRepository {

    private static final long serialVersionUID = -6614056402124460918L;

    private final File cacheDir;
    private final File workingCopyRoot;
    private transient Repository gitRepository;
    private final Set<String> analyzedCommits;
    private IMutableFileHistoryGraph fileHistoryGraph;

    /**
     * Constructor of the {@link GitRepository}.
     */
    private GitRepository(final File workingCopyRoot, File cacheDir) {
        this.workingCopyRoot = workingCopyRoot;
        this.cacheDir = cacheDir;

        this.analyzedCommits = new HashSet<>();
        this.fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    }

    public static GitRepository create(final File workingCopyRoot, File cacheDir) {
        final GitRepository fromCache = loadFromCache(workingCopyRoot, cacheDir);
        if (fromCache != null && fromCache.workingCopyRoot.equals(workingCopyRoot)) {
            return fromCache;
        } else {
            return new GitRepository(workingCopyRoot, cacheDir);
        }
    }

    Repository getRepository() {
        if (this.gitRepository == null) {
            try {
                this.gitRepository = new FileRepositoryBuilder().findGitDir(this.workingCopyRoot).build();
            } catch (final IOException e) {
                throw new ReviewtoolException(e);
            }
        }
        return this.gitRepository;
    }

    @SuppressWarnings("unchecked")
    private static GitRepository loadFromCache(File wcRoot, File cacheDir) {
        final File cacheFile = getCacheFilePath(wcRoot, cacheDir);
        if (cacheFile.exists()) {
            try (FileInputStream in = new FileInputStream(cacheFile)) {
                final ObjectInputStream os = new ObjectInputStream(new BufferedInputStream(in));
                return (GitRepository) os.readObject();
            } catch (final Exception e) {
                Logger.warn("could not load git repo cache file", e);
            }
        }
        return null;
    }

    void saveCache() {
        try (FileOutputStream in = new FileOutputStream(getCacheFilePath(this.workingCopyRoot, this.cacheDir))) {
            final ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(in));
            os.writeObject(this);
            os.close();
        } catch (final Exception e) {
            Logger.warn("could not save git repo cache file", e);
        }
    }

    @Override
    public String getId() {
        // as each working copy is also a repository, and there is nothing like a central repository
        //  in git, just use the local path as ID
        return this.workingCopyRoot.getAbsolutePath();
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision<?> revision) throws Exception {
        try (ObjectReader reader = this.getRepository().newObjectReader();
                final RevWalk walk = new RevWalk(reader)) {
            // Get the commit object for that revision
            final ObjectId commitId = this.getRepository().resolve(((RevisionId) revision.getId()).getName());
            if (commitId == null) {
                return new byte[0];
            }
            final RevCommit commit = walk.parseCommit(commitId);

            // Get the revision's file tree
            final RevTree tree = commit.getTree();
            // .. and narrow it down to the single file's path
            final TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

            if (treewalk != null) {
                // use the blob id to read the file's data
                return reader.open(treewalk.getObjectId(0)).getBytes();
            } else {
                return new byte[0];
            }
        }
    }

    @Override
    public IMutableFileHistoryGraph getFileHistoryGraph() {
        return this.fileHistoryGraph;
    }

    private static File getCacheFilePath(File wcRoot, File cacheDir) {
        return new File(cacheDir, "git-" + encodeString(wcRoot.toString()) + ".cache");
    }

    private static String encodeString(final String s) {
        return Base64.getUrlEncoder().encodeToString(s.getBytes());
    }

    boolean wasAlreadyAnalyzed(String revisionString) {
        return this.analyzedCommits.contains(revisionString);
    }

    void markAsAnalyzed(String revisionString) {
        this.analyzedCommits.add(revisionString);
    }

    public void clearCache() {
        final File filePath = getCacheFilePath(this.workingCopyRoot, this.cacheDir);
        filePath.delete();
        if (!this.analyzedCommits.isEmpty()) {
            this.analyzedCommits.clear();
            this.fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
        }
    }

}
