package de.setsoftware.reviewtool.irrelevancestrategies.pathfilters;

import java.util.HashSet;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IChange;
import de.setsoftware.reviewtool.model.api.ICommit;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;

/**
 * Filter for changes in commits that contains more than a certain threshold of files.
 */
public class FileCountInCommitFilter extends IIrrelevanceDetermination {

    private final String description;
    private final int threshold;

    private ICommit curCommit;
    private int fileCountInCurCommit;

    public FileCountInCommitFilter(int number, String description, int threshold) {
        super(number);
        this.description = description;
        this.threshold = threshold;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void clearCaches() {
        this.curCommit = null;
        this.fileCountInCurCommit = -1;
    }

    @Override
    public boolean isIrrelevant(ICommit commit, IChange change) {
        if (this.curCommit != commit) {
            this.curCommit = commit;
            this.fileCountInCurCommit = this.countFilesIn(commit);
        }
        return this.fileCountInCurCommit >= this.threshold;
    }

    private int countFilesIn(ICommit commit) {
        final Set<IRevisionedFile> files = new HashSet<>();
        for (final IChange c : commit.getChanges()) {
            files.add(c.getTo());
        }
        return files.size();
    }

}
