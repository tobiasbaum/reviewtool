package de.setsoftware.reviewtool.changesources.core;

import java.util.regex.Pattern;

/**
 * Default commit filter that looks for a pattern in the commit message only.
 *
 * @param <ItemT> Type of a change item.
 * @param <CommitIdT> Type of a commit ID.
 * @param <CommitT> Type of a commit.
 */
final class DefaultScmCommitFilter<
        ItemT extends IScmChangeItem,
        CommitIdT extends IScmCommitId<CommitIdT>,
        CommitT extends IScmCommit<ItemT, CommitIdT>>
            implements IScmCommitHandler<ItemT, CommitIdT, CommitT, Boolean> {

    private final Pattern pattern;

    /**
     * Constructor.
     *
     * @param pattern The pattern to search for in the commit message.
     */
    DefaultScmCommitFilter(final Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public Boolean processCommit(final CommitT commit) {
        final String message = commit.getMessage();
        return message != null && this.pattern.matcher(message).matches();
    }
}
