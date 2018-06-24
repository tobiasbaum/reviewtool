package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IChangeData;
import de.setsoftware.reviewtool.model.api.IChangeSource;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IRepository;

/**
 * Implementation of {@link IChangeData}.
 */
public final class ChangeData implements IChangeData {

    private final IChangeSource source;
    private final List<? extends ICommit> commits;
    private final Set<IRepository> repositories;

    ChangeData(final IChangeSource source, final List<? extends ICommit> commits) {
        this.source = source;
        this.commits = commits;

        this.commits.sort(new Comparator<ICommit>() {
            @Override
            public int compare(final ICommit o1, final ICommit o2) {
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
}
