package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;

/**
 * Default implementation of @{link ICommit}.
 */
public class Commit implements ICommit {

    private final String message;
    private final List<IChange> changes;
    private final boolean isVisible;

    Commit(String message, List<? extends IChange> changes, final boolean isVisible) {
        this.message = message;
        this.changes = new ArrayList<>(changes);
        this.isVisible = isVisible;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public List<? extends IChange> getChanges() {
        return Collections.unmodifiableList(this.changes);
    }

    @Override
    public Commit makeChangesIrrelevant(Set<? extends IChange> toMakeIrrelevant) {
        final List<IChange> adjustedChanges = new ArrayList<>();
        boolean isVisible = false;
        for (final IChange change : this.changes) {
            if (toMakeIrrelevant.contains(change)) {
                adjustedChanges.add(change.makeIrrelevant());
            } else {
                adjustedChanges.add(change);
            }
            isVisible = isVisible || change.isVisible();
        }
        return new Commit(this.message, adjustedChanges, isVisible);
    }

    @Override
    public boolean isVisible() {
        return this.isVisible;
    }

}
