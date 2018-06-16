package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;

/**
 * Default implementation of @{link ICommit}.
 */
public class Commit implements ICommit {

    private final IWorkingCopy wc;
    private final String message;
    private final List<IChange> changes;
    private final IRevision revision;
    private final long timestamp;

    Commit(
            final IWorkingCopy wc,
            final String message,
            final List<? extends IChange> changes,
            final IRevision revision,
            final Date timestamp) {

        this.wc = wc;
        this.message = message;
        this.changes = new ArrayList<>(changes);
        this.revision = revision;
        this.timestamp = timestamp.getTime();
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public IRevision getRevision() {
        return this.revision;
    }

    @Override
    public List<? extends IChange> getChanges() {
        return Collections.unmodifiableList(this.changes);
    }

    @Override
    public Commit makeChangesIrrelevant(Set<? extends IChange> toMakeIrrelevant) {
        final List<IChange> adjustedChanges = new ArrayList<>();
        for (final IChange change : this.changes) {
            if (toMakeIrrelevant.contains(change)) {
                adjustedChanges.add(change.makeIrrelevant());
            } else {
                adjustedChanges.add(change);
            }
        }
        return new Commit(this.wc, this.message, adjustedChanges, this.revision, this.getTime());
    }

    @Override
    public Date getTime() {
        return new Date(this.timestamp);
    }

    @Override
    public IWorkingCopy getWorkingCopy() {
        return this.wc;
    }
}
