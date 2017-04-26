package de.setsoftware.reviewtool.model.changestructure;

import java.util.Collections;
import java.util.Set;

/**
 * A node in a {@link FileHistoryGraph} which denotes a deleted file.
 */
public final class NonExistingFileHistoryNode extends FileHistoryNode {

    public NonExistingFileHistoryNode(final FileInRevision file) {
        super(file);
    }

    @Override
    public Set<IFileHistoryEdge> getDescendants() {
        return Collections.emptySet();
    }

    @Override
    public void accept(FileHistoryNodeVisitor v) {
        v.handleNonExistingNode(this);
    }
}
