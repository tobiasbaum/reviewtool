package de.setsoftware.reviewtool.changesources.core;

import java.util.Date;
import java.util.Map;

import de.setsoftware.reviewtool.changesources.core.IScmCommit;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRepoRevision;
import de.setsoftware.reviewtool.model.api.IRepository;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

public final class StubCommit implements IScmCommit<StubChangeItem, StubCommitId> {

    private static final long serialVersionUID = 5661467748772243268L;
    private final IRepository repo;
    private final Map<String, StubChangeItem> changeItems;
    private final StubCommitId commitId;
    private final String message;
    private final String author;
    private final Date date;

    /**
     * Constructor.
     *
     * @param repo The associated repository.
     * @param changeItems The change items.
     */
    public StubCommit(
            final IRepository repo,
            final Map<String, StubChangeItem> changeItems,
            final long id,
            final String message,
            final String author,
            final Date date) {
        this.repo = repo;
        this.changeItems = changeItems;
        this.commitId = new StubCommitId(id);
        this.message = message;
        this.author = author;
        this.date = date;
    }

    @Override
    public Map<String, StubChangeItem> getChangedItems() {
        return this.changeItems;
    }

    @Override
    public void integrateInto(final IMutableFileHistoryGraph graph) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IRepository getRepository() {
        return this.repo;
    }

    @Override
    public StubCommitId getId() {
        return this.commitId;
    }

    @Override
    public String getCommitter() {
        return this.author;
    }

    @Override
    public Date getCommitDate() {
        return this.date;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public IRepoRevision<StubCommitId> toRevision() {
        return ChangestructureFactory.createRepoRevision(this.commitId, this.repo);
    }
}
