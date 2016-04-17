package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A commit (SCM transaction) with the changes performed in it.
 */
public class Commit {

    private final String message;
    private final List<Change> changes;

    public Commit(String message, List<Change> changes) {
        this.message = message;
        this.changes = new ArrayList<Change>(changes);
    }

    public String getMessage() {
        return this.message;
    }

    public List<Change> getChanges() {
        return Collections.unmodifiableList(this.changes);
    }

}
