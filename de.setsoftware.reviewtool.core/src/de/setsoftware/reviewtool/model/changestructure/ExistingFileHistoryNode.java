package de.setsoftware.reviewtool.model.changestructure;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A node in a {@link FileHistoryGraph} which denotes an existing revisioned file.
 * It has a list of descendant {@link FileHistoryNode}s this file evolves to due to changes, copies, or
 * deletions.
 */
public final class ExistingFileHistoryNode extends FileHistoryNode {

    private final Set<FileHistoryEdge> descendants;

    public ExistingFileHistoryNode(final FileInRevision file) {
        super(file);
        this.descendants = new LinkedHashSet<>();
    }

    @Override
    protected void attributesToString(final List<String> attributes) {
        super.attributesToString(attributes);
        if (!this.descendants.isEmpty()) {
            final Set<IFileHistoryNode> nodes = new LinkedHashSet<>();
            for (final IFileHistoryEdge descendantEdge : this.descendants) {
                nodes.add(descendantEdge.getDescendant());
            }
            attributes.add("descendants=" + nodes);
        }
    }

    @Override
    public Set<FileHistoryEdge> getDescendants() {
        return this.descendants;
    }

    /**
     * Adds a descendant {@link FileHistoryNode} of this node.
     */
    public void addDescendant(final FileHistoryNode descendant, final FileDiff diff) {
        final FileHistoryEdge edge = new FileHistoryEdge(this, descendant, diff);
        this.descendants.add(edge);
        descendant.addAncestor(edge);
    }

    @Override
    public void accept(FileHistoryNodeVisitor v) {
        v.handleExistingNode(this);
    }
}
