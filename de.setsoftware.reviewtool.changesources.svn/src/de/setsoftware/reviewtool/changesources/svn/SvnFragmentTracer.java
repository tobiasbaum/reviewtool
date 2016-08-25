package de.setsoftware.reviewtool.changesources.svn;

import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileChangeHistory;
import de.setsoftware.reviewtool.model.changestructure.FileDiff;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.Hunk;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;
import de.setsoftware.reviewtool.model.changestructure.IncompatibleFragmentException;
import de.setsoftware.reviewtool.model.changestructure.RepositoryChangeHistory;

/**
 * A simple svn based fragment tracer that does not trace position changes and only traces file renames.
 */
public class SvnFragmentTracer implements IFragmentTracer {

    final RepositoryChangeHistory repoChangeHistory;

    public SvnFragmentTracer(final RepositoryChangeHistory repoChangeHistory) {
        this.repoChangeHistory = repoChangeHistory;
    }

    @Override
    public Fragment traceFragment(Fragment fragment) {
        final FileChangeHistory changeHistory = this.repoChangeHistory.getHistory(fragment.getFile());
        if (changeHistory != null) {
            try {
                final FileDiff fileDiff = changeHistory.build(fragment.getFile(), changeHistory.getLastRevision());
                final Hunk hunk = fileDiff.getHunkForSource(fragment);
                if (hunk != null) {
                    final Fragment lastFragment = hunk.getTarget();
                    return ChangestructureFactory.createFragment(
                            changeHistory.getLastRevision(),
                            lastFragment.getFrom(),
                            lastFragment.getTo(),
                            lastFragment.getContent());
                }
            } catch (final IncompatibleFragmentException e) {
                // fall through
            }
        }

        // safety belt
        return ChangestructureFactory.createFragment(
                this.traceFile(fragment.getFile()),
                fragment.getFrom(),
                fragment.getTo(),
                fragment.getContent());
    }


    @Override
    public FileInRevision traceFile(FileInRevision file) {
        return this.repoChangeHistory.getHistory(file).getLastRevision();
    }
}
