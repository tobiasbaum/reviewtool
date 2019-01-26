package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

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

    private final Repository gitRepository;
    private final IMutableFileHistoryGraph fileHistoryGraph;

    /**
     * Constructor of the {@link GitRepository}.
     */
    GitRepository(final File workingCopyRoot) {
        try {
            this.gitRepository = new FileRepositoryBuilder().findGitDir(workingCopyRoot).build();
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }

        this.fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    }

    @Override
    public String getId() {
        // Git repositories do not have a unique id.... So we just return the URL of the
        // origin server - which is quite unique, too. If no remote origin is set, we just
        // return the local path of the reposiory.... this is at least locally unique...
        final String remoteUrl = this.gitRepository.getConfig().getString("remote", "origin", "url");
        return remoteUrl != null ? remoteUrl : this.gitRepository.getDirectory().getAbsolutePath();
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision<?> revision) throws Exception {
        try (ObjectReader reader = this.gitRepository.newObjectReader();
                final RevWalk walk = new RevWalk(reader)) {
            // Get the commit object for that revision
            final ObjectId commitId = this.gitRepository.resolve(((RevisionId) revision.getId()).getName());
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

    public Repository getRepository() {
        return this.gitRepository;
    }
}
