package de.setsoftware.reviewtool.model.changestructure;

/**
 * Interface for tracing fragments between revisions.
 */
public interface IFragmentTracer {

    /**
     * Determines the fragment that most closely represents the given fragment in the given revision.
     * If the fragment already denotes the given revision, this is a no-op.
     */
    public abstract Fragment getInRevision(Fragment fragment, Revision revision);

}
