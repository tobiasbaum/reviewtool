package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A fragment tracer based on a {@link IFileHistoryGraph}.
 */
public class FragmentTracer implements IFragmentTracer {

    private final IFileHistoryGraph fileHistoryGraph;

    public FragmentTracer(final IFileHistoryGraph fileHistoryGraph) {
        this.fileHistoryGraph = fileHistoryGraph;
    }

    @Override
    public List<Fragment> traceFragment(Fragment fragment) {
        final ArrayList<Fragment> result = new ArrayList<>();
        final IFileHistoryNode node = this.fileHistoryGraph.getNodeFor(fragment.getFile());
        if (node != null) {
            for (final FileInRevision leafRevision : this.fileHistoryGraph.getLatestFiles(node.getFile())) {
                final IFileHistoryNode descendant = this.fileHistoryGraph.getNodeFor(leafRevision);
                final Set<FileDiff> fileDiffs = descendant.buildHistories(node);
                for (final FileDiff fileDiff : fileDiffs) {
                    final Fragment lastFragment = fileDiff.traceFragment(fragment);
                    result.add(lastFragment);
                }
            }
        }

        return result;
    }

    @Override
    public List<FileInRevision> traceFile(FileInRevision file) {
        final ArrayList<FileInRevision> result = new ArrayList<>();
        final IFileHistoryNode node = this.fileHistoryGraph.getNodeFor(file);
        if (node != null) {
            for (final FileInRevision leafRevision : this.fileHistoryGraph.getLatestFiles(file)) {
                result.add(leafRevision);
            }
        }

        return result;
    }
}
