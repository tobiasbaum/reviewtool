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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

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

    private final File workingCopyRoot;
    private transient Repository gitRepository;
    private final Set<String> analyzedCommits;
    private IMutableFileHistoryGraph fileHistoryGraph;

    /**
     * Constructor of the {@link GitRepository}.
     */
    private GitRepository(final File workingCopyRoot) {
        this.workingCopyRoot = workingCopyRoot;

        this.analyzedCommits = new HashSet<>();
        this.fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    }

    public static GitRepository create(final File workingCopyRoot) {
        final GitRepository fromCache = loadFromCache(workingCopyRoot);
        if (fromCache != null && fromCache.workingCopyRoot.equals(workingCopyRoot)) {
            return fromCache;
        } else {
            return new GitRepository(workingCopyRoot);
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
    private static GitRepository loadFromCache(File wcRoot) {
        final File cacheFile = getCacheFilePath(wcRoot);
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
        try (FileOutputStream in = new FileOutputStream(getCacheFilePath(this.workingCopyRoot))) {
            final ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(in));
            os.writeObject(this);
            os.close();
        } catch (final Exception e) {
            Logger.warn("could not save git repo cache file", e);
        }
    }

    @Override
    public String getId() {
        // Git repositories do not have a unique id.... So we just return the URL of the
        // origin server - which is quite unique, too. If no remote origin is set, we just
        // return the local path of the reposiory.... this is at least locally unique...
        final String remoteUrl = this.getRepository().getConfig().getString("remote", "origin", "url");
        return remoteUrl != null ? remoteUrl : this.getRepository().getDirectory().getAbsolutePath();
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

    private static File getCacheFilePath(File wcRoot) {
        if (!Platform.isRunning()) {
            return new File("gitHistory.cache");
        }
        final Bundle bundle = FrameworkUtil.getBundle(GitRepository.class);
        final IPath dir = Platform.getStateLocation(bundle);
        return dir.append("git-" + encodeString(wcRoot.toString()) + ".cache").toFile();
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
        final File filePath = getCacheFilePath(this.workingCopyRoot);
        filePath.delete();
        if (!this.analyzedCommits.isEmpty()) {
            this.analyzedCommits.clear();
            this.fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
        }
    }

}
