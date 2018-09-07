package de.setsoftware.reviewtool.changesources.svn;

import java.util.Collections;
import java.util.Set;

import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.w3c.dom.Element;

import de.setsoftware.reviewtool.changesources.core.DefaultChangeSource;
import de.setsoftware.reviewtool.changesources.core.IScmRepositoryBridge;
import de.setsoftware.reviewtool.changesources.core.IScmWorkingCopyBridge;
import de.setsoftware.reviewtool.config.IConfigurator;
import de.setsoftware.reviewtool.config.IReviewConfigurable;
import de.setsoftware.reviewtool.model.api.IChangeSource;

/**
 * Configurator for this package.
 */
public class SvnChangesourceConfigurator implements IConfigurator {

    @Override
    public Set<String> getRelevantElementNames() {
        return Collections.singleton("svnChangeSource");
    }

    @Override
    public void configure(final Element xml, final IReviewConfigurable configurable) {

        final String user = xml.getAttribute("user");
        final String pwd = xml.getAttribute("password");
        final String pattern = xml.getAttribute("pattern");
        final String maxTextDiffThreshold = xml.getAttribute("maxTextDiffFileSizeThreshold");
        final String minLogCacheSize = xml.getAttribute("minLogCacheSize");

        final SVNClientManager mgr = SVNClientManager.newInstance();
        mgr.setAuthenticationManager(new DefaultSVNAuthenticationManager(
                null, false, user, pwd.toCharArray(), null, null));

        final IScmRepositoryBridge<SvnChangeItem, SvnCommitId, SvnCommit, SvnRepository> scmRepoBridge =
                new SvnRepositoryBridge(mgr);
        final IScmWorkingCopyBridge<SvnChangeItem, SvnCommitId, SvnCommit, SvnRepository, SvnLocalChange,
                SvnWorkingCopy> scmWcBridge = new SvnWorkingCopyBridge(mgr);
        final IChangeSource changeSource = new DefaultChangeSource<>(
                scmRepoBridge,
                scmWcBridge,
                pattern,
                Long.parseLong(maxTextDiffThreshold),
                minLogCacheSize.isEmpty() ? 1000 : Integer.parseInt(minLogCacheSize));

        configurable.setChangeSource(changeSource);
    }
}
