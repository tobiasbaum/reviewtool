package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a container for {@link FileChangeHistory} objects.
 */
public class RepositoryChangeHistory {

    /**
     * Maps a path to a set of {@link FileInRevision}s.
     */
    private final Map<String, List<FileInRevision>> fileRevisions;
    /**
     * The change histories per file.
     */
    final Map<FileInRevision, FileChangeHistory> changeHistories;

    /**
     * Creates an empty change history.
     */
    public RepositoryChangeHistory() {
        this.fileRevisions = new LinkedHashMap<>();
        this.changeHistories = new LinkedHashMap<>();
    }

    /**
     * Creates a change history from a collection of {@link Commit}s.
     *
     * @param commits The commits to import.
     */
    public RepositoryChangeHistory(final Collection<? extends Commit> commits) {
        this();
        for (final Commit commit : commits) {
            for (final Change change : commit.getChanges()) {
                RepositoryChangeHistory.this.addChange(change);
            }
        }
    }

    /**
     * Maps a {@link FileInRevision} to a {@link FileChangeHistory}.
     * @param file The {@link FileInRevision} object.
     * @return The {@link FileChangeHistory} describing changes for passed {@link FileInRevision} or null if not found.
     */
    public FileChangeHistory getHistory(final FileInRevision file) {
        return this.changeHistories.get(file);
    }

    /**
     * Adds a {@link Change} to this change history.
     * @param change The {@link Change} to add.
     */
    public void addChange(final Change change) {
        change.accept(new ChangeVisitor() {

            @Override
            public void handle(BinaryChange visitee) {
                RepositoryChangeHistory.this.getChangeHistory(visitee.getFrom(), visitee.getTo());
            }

            @Override
            public void handle(TextualChangeHunk visitee) {
                RepositoryChangeHistory.this.getChangeHistory(visitee.getFromFragment().getFile(),
                        visitee.getToFragment().getFile()).add(new Hunk(visitee));
            }
        });
    }

    /**
     * Returns the {@link FileChangeHistory} given two {@link FileInRevision}s. The internal data structures are
     * adjusted such that the change history is linked to the old and the new revision.
     * @param fromFile The source file.
     * @param toFile The target file.
     * @return The corresponding {@link FileHistoryChange} object. If no such object exists yet, it is created and
     *          added.
     */
    private FileChangeHistory getChangeHistory(final FileInRevision fromFile, final FileInRevision toFile) {
        List<FileInRevision> files = this.fileRevisions.get(fromFile.getPath());
        if (files == null) {
            files = new ArrayList<>();
            files.add(fromFile);
            this.fileRevisions.put(fromFile.getPath(), files);
            this.changeHistories.put(fromFile, new FileChangeHistory());
        } else if (!this.changeHistories.containsKey(fromFile)) {
            files.add(fromFile);
            this.changeHistories.put(fromFile, this.changeHistories.get(files.get(0)));
        }
        if (!this.fileRevisions.containsKey(toFile.getPath())) {
            this.fileRevisions.put(toFile.getPath(), files);
        }
        if (!this.changeHistories.containsKey(toFile)) {
            files.add(toFile);
            this.changeHistories.put(toFile, this.changeHistories.get(fromFile));
        }
        return this.changeHistories.get(toFile);
    }

}
