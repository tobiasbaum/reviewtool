package de.setsoftware.reviewtool.changesources.git;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

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
            this.gitRepository = FileRepositoryBuilder.create(workingCopyRoot);
        } catch (final IOException e) {
            throw new ReviewtoolException(e);
        }

        this.fileHistoryGraph = new FileHistoryGraph(DiffAlgorithmFactory.createDefault());
    }

    @Override
    public String getId() {
        // Git repositories do not have a unqique id.... So we just return the URL of the
        // origin server - which is quite unique, too. If no remote origin is set, we just
        // return the local path of the reposiory.... this is at least locally unique...
        final String remoteUrl = this.gitRepository.getConfig().getString("remote", "origin", "url");
        return remoteUrl != null ? remoteUrl : this.gitRepository.getDirectory().getAbsolutePath();
    }

    @Override
    public byte[] getFileContents(final String path, final IRepoRevision revision) throws Exception {
        // TODO implement getFileContents()
        // See http://www.vogella.com/tutorials/JGit/article.html
        return null;
    }

    @Override
    public IMutableFileHistoryGraph getFileHistoryGraph() {
        // TODO implement getFileHistoryGraph()
        return null;
    }

    public Repository getRepository() {
        return this.gitRepository;
    }
}
