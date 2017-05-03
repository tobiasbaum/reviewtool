package de.setsoftware.reviewtool.changesources.svn;

/**
 * Visitor for {@link ISvnRevision}.
 */
public interface ISvnRevisionVisitor {

    /**
     * Handles a {@link SvnRevision}.
     */
    public abstract void handle(SvnRevision revision);

    /**
     * Handles a {@link WorkingCopyRevision}.
     */
    public abstract void handle(WorkingCopyRevision revision);

}
