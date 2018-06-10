package de.setsoftware.reviewtool.model.changestructure;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
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
 * Implementation of {@link IChangeData}.
 */
public final class ChangeData implements IChangeData {

    private final IChangeSource source;
    private final List<? extends ICommit> commits;
    private final Map<File, IRevisionedFile> localPathMap;
    private final Set<IRepository> repositories;

    ChangeData(final IChangeSource source, final List<? extends ICommit> commits) {
        this(source, commits, Collections.<File, IRevisionedFile> emptyMap());
    }

    ChangeData(
            final IChangeSource source,
            final List<? extends ICommit> commits,
            final Map<File, IRevisionedFile> localPathMap) {

        this.source = source;
        this.commits = commits;
        this.localPathMap = localPathMap;

        this.commits.sort(new Comparator<ICommit>() {
            @Override
            public int compare(ICommit o1, ICommit o2) {
                return o1.getTime().compareTo(o2.getTime());
            }
        });

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
