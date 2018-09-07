package de.setsoftware.reviewtool.changesources.svn;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;

import de.setsoftware.reviewtool.changesources.core.DefaultScmRepository;
import de.setsoftware.reviewtool.changesources.core.ScmException;
import de.setsoftware.reviewtool.model.api.IChangeSource;

/**
 * Implements {@link DefaultScmRepository} for Subversion.
 */
final class SvnRepository extends DefaultScmRepository<SvnChangeItem, SvnCommitId, SvnCommit, SvnRepository> {

    private static final long serialVersionUID = -2718539486699478329L;
    private final SVNRepository svnRepo;

    /**
     * Constructor.
     * @param changeSource The associated change source.
     * @param url The URL pointing to the remote Subversion repository.
     * @param scmRepoBridge The {@link SvnRepositoryBridge} to use.
     */
    SvnRepository(
            final IChangeSource changeSource,
            final SVNURL url,
            final SvnRepositoryBridge scmRepoBridge) throws ScmException {
        super(changeSource, url.toString(), scmRepoBridge);
        try {
            this.svnRepo = scmRepoBridge.getSvnManager().createRepository(url, false);
        } catch (final SVNException e) {
            throw new ScmException(e);
        }
    }

    /**
     * Returns the underlying {@link SVNRepository}.
     */
    SVNRepository getSvnRepository() {
        return this.svnRepo;
    }

    @Override
    public SvnRepository getThis() {
        return this;
    }
}
