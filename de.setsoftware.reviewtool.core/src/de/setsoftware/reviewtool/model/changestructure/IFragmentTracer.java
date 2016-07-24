package de.setsoftware.reviewtool.model.changestructure;

/**
 * Interface for tracing fragments between revisions.
 * Subclasses can assume that a new tracer is created every time a new review/fixing starts.
 */
public interface IFragmentTracer {

    /**
     * Determines the fragment that most closely represents the given fragment in the most recent revision.
     * If the fragment already denotes the most recent revision, this is a no-op.
     */
    public abstract Fragment traceFragment(Fragment fragment);

    /**
     * Determines the fragment that most closely represents the given file in the most recent revision.
     * If the fragment already denotes the most recent revision, this is a no-op.
     */
    public abstract FileInRevision traceFile(FileInRevision fragment);

}
