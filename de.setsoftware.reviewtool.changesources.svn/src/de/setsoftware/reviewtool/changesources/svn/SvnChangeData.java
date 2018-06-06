package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Implementation of {@link IChangeData} that caches data needed for tracing in addition to the matched commits.
 */
final class SvnChangeData implements IChangeData {

    private final IChangeSource source;
    private final List<? extends ICommit> commits;
    private final Map<File, IRevisionedFile> localPathMap;
    private final Set<IRepository> repositories;

    SvnChangeData(
            final IChangeSource source,
            final List<? extends ICommit> commits) {

        this(source, commits, Collections.<File, IRevisionedFile> emptyMap());
    }

    SvnChangeData(
            final IChangeSource source,
            final List<? extends ICommit> commits,
            final Map<File, IRevisionedFile> localPathMap) {

        this.source = source;
        this.commits = commits;
        this.localPathMap = localPathMap;

        this.repositories = new LinkedHashSet<>();
        for (final ICommit commit : this.commits) {
            this.repositories.add(commit.getRevision().getRepository());
        }
    }

    @Override
    public IChangeSource getSource() {
        return this.source;
    }

    @Override
    public Set<? extends IRepository> getRepositories() {
        return Collections.unmodifiableSet(this.repositories);
    }

    @Override
    public List<? extends ICommit> getMatchedCommits() {
        return Collections.unmodifiableList(this.commits);
    }

    @Override
    public Map<File, IRevisionedFile> getLocalPathMap() {
        return Collections.unmodifiableMap(this.localPathMap);
    }

}
