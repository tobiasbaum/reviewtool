package de.setsoftware.reviewtool.model.changestructure;

import java.util.List;

/**
 * Interface for tracing fragments between revisions.
 * Subclasses can assume that a new tracer is created every time a new review/fixing starts.
 */
public interface IFragmentTracer {

    /**
     * Determines the target fragment that most closely represents the given source fragment in the most recent
     * revision. If the fragment already denotes the most recent revision, this is an identity.
     */
    public abstract List<Fragment> traceFragment(Fragment fragment);

    /**
     * Determines the target file that most closely represents the given source file in the most recent revision.
     * If the file already denotes the most recent revision, this is an identity.
     */
    public abstract List<FileInRevision> traceFile(FileInRevision fragment);

}
