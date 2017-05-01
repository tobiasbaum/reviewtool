package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Contains behaviour common to all {@link IFileHistoryNode} implementations.
 */
public abstract class AbstractFileHistoryNode implements IFileHistoryNode {

    private static final ThreadLocal<Boolean> inToString = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    @Override
    public final Set<FileDiff> buildHistories(final IFileHistoryNode from) {
        if (from.equals(this)) {
            return Collections.singleton(new FileDiff(from.getFile(), from.getFile()));
        }

        if (!this.isRoot()) {
            final Set<FileDiff> result = new LinkedHashSet<>();
            for (final IFileHistoryEdge ancestorEdge : this.getAncestors()) {
                for (final FileDiff diff : ancestorEdge.getAncestor().buildHistories(from)) {
                    try {
                        result.add(diff.merge(ancestorEdge.getDiff()));
                    } catch (final IncompatibleFragmentException e) {
                        throw new ReviewtoolException(e);
                    }
                }
            }
            return result;
        } else {
            return Collections.emptySet(); // ancestor revision not found
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * In order to prevent infinite recursion, a thread-local flag <code>inToString</code> is used to detect cycles.
     */
    @Override
    public String toString() {
        if (inToString.get()) {
            return this.getFile().toString();
        }

        inToString.set(true);
        try {
            final List<String> attributes = new ArrayList<>();
            this.attributesToString(attributes);
            if (attributes.isEmpty()) {
                return this.getFile().toString();
            } else {
                return this.getFile().toString() + attributes.toString();
            }
        } finally {
            inToString.set(false);
        }
    }

    /**
     * Fills a list of additional attributes used by toString().
     * @param attributes A list containing elements to be included in the output of {@link #toString()}.
     */
    protected void attributesToString(final List<String> attributes) {
        if (!this.isRoot()) {
            final Set<IFileHistoryNode> nodes = new LinkedHashSet<>();
            for (final IFileHistoryEdge ancestorEdge : this.getAncestors()) {
                nodes.add(ancestorEdge.getAncestor());
            }
            attributes.add("ancestors=" + nodes);
        }
        final Set<? extends IFileHistoryEdge> descendants = this.getDescendants();
        if (!descendants.isEmpty()) {
            final Set<IFileHistoryNode> nodes = new LinkedHashSet<>();
            for (final IFileHistoryEdge descendantEdge : descendants) {
                nodes.add(descendantEdge.getDescendant());
            }
            attributes.add("descendants=" + nodes);
        }
        if (this.isDeleted()) {
            attributes.add("deleted");
        }
    }

}
