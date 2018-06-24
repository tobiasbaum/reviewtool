package de.setsoftware.reviewtool.model.api;

import java.util.List;

/**
 * Interface for tracing fragments between revisions.
 * Subclasses can assume that a new tracer is created every time a new review/fixing starts.
 */
public interface IFragmentTracer {

    /**
     * Determines the target fragment that most closely represents the given source fragment in the most recent
     * revision. If the fragment already denotes the most recent revision, this is an identity.
     *
     * @param fileHistoryGraph The file history graph to use for tracing.
     * @param fragment The source fragment to start with.
     * @param ignoreNonLocalCopies If {@code true}, non-local copies are ignored while tracing.
     */
    public abstract List<? extends IFragment> traceFragment(
            IFileHistoryGraph fileHistoryGraph,
            IFragment fragment,
            boolean ignoreNonLocalCopies);

    /**
     * Determines the target file that most closely represents the given source file in the most recent revision.
     * If the file already denotes the most recent revision, this is an identity.
     *
     * @param fileHistoryGraph The file history graph to use for tracing.
     * @param file The source file to start with.
     * @param ignoreNonLocalCopies If {@code true}, non-local copies are ignored while tracing.
     */
    public abstract List<IRevisionedFile> traceFile(
            IFileHistoryGraph fileHistoryGraph,
            IRevisionedFile file,
            boolean ignoreNonLocalCopies);
}
