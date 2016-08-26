package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import de.setsoftware.reviewtool.base.Multimap;

/**
 * Represents a container for {@link Hunk}s. Contrary to a {@link FileDiff}, the changes are not merged
 * but kept separately. This allows to generate {@link FileDiff}s on-the-fly for arbitrary from/to revisions.
 */
public class FileChangeHistory {

    /**
     * The hunks per file revision.
     */
    private final Multimap<FileInRevision, Hunk> history;
    /**
     * The revisions being part of this change history.
     */
    private List<FileInRevision> revisions;
    /**
     * Stores whether the list of revisions is sorted.
     */
    private boolean isSorted;

    /**
     * Creates an empty change history.
     */
    public FileChangeHistory() {
        this.history = new Multimap<>();
        this.revisions = new ArrayList<>();
        this.isSorted = true;
    }

    /**
     * @return The first revision of the file.
     */
    public FileInRevision getFirstRevision() {
        return this.revisions.isEmpty() ? null : this.revisions.get(0);
    }

    /**
     * @return The last revision of the file.
     */
    public FileInRevision getLastRevision() {
        return this.revisions.isEmpty() ? null : this.revisions.get(this.revisions.size() - 1);
    }

    /**
     * Adds a binary file to this change history.
     */
    public void add(final FileInRevision sourceRev) {
        if (!this.revisions.contains(sourceRev)) {
            this.revisions.add(sourceRev);
            this.isSorted = false;
        }
    }

    /**
     * Adds a hunk to this change history.
     * @param hunk The hunk to add.
     */
    public void add(final Hunk hunk) {
        final FileInRevision sourceRev = hunk.getSource().getFile();
        this.add(sourceRev);
        this.history.put(sourceRev, hunk);
    }

    /**
     * Adds a list of hunks to this change history.
     * @param hunks The hunks to add.
     */
    public void addAll(final Collection<? extends Hunk> hunks) {
        for (final Hunk hunk : hunks) {
            this.add(hunk);
        }
    }

    /**
     * Adds all hunks of another {@link FileChangeHistory} to this change history.
     * @param history The {@link FileChangeHistory} whose hunks are to be added.
     */
    public void addHistory(final FileChangeHistory history) {
        for (final FileInRevision rev : history.revisions) {
            this.addAll(history.history.get(rev));
        }
    }

    /**
     * Builds a {@link FileDiff} from this change history, given a start and end revision. In contrast to this object,
     * a {@link FileDiff} merges the relevant hunks, thereby providing a unified view of changes from start to end
     * revision.
     *
     * @param fromFile The start revision (inclusive).
     * @param toFile The end revision (inclusive).
     * @return A {@link FileDiff} object containing the merged hunks. If the start revision is not found,
     *      <code>null</code> is returned. If the end revision is not found, the last revision of this change history is
     *      used as end revision.
     * @throws IncompatibleFragmentException if an error building the {@link FileDiff} object occurred.
     */
    public FileDiff build(final FileInRevision fromFile, final FileInRevision toFile)
            throws IncompatibleFragmentException {
        if (!this.isSorted) {
            this.revisions = FileInRevision.sortByRevision(this.revisions);
            this.isSorted = true;
        }

        final ListIterator<FileInRevision> it = this.revisions.listIterator();
        while (it.hasNext()) {
            if (it.next().equals(fromFile)) {
                it.previous();
                break;
            }
        }

        if (!it.hasNext()) {
            return null; // source revision not found
        }

        FileDiff diff = new FileDiff();
        while (it.hasNext()) {
            final List<Hunk> hunks = this.history.get(it.next());
            diff = diff.merge(hunks);
            if (hunks.get(0).getTarget().getFile().equals(toFile)) {
                break;
            }
        }

        return diff;
    }

}
