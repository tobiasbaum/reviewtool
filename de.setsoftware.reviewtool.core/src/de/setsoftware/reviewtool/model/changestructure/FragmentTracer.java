package de.setsoftware.reviewtool.model.changestructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.setsoftware.reviewtool.model.api.IFileDiff;
import de.setsoftware.reviewtool.model.api.IFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IFileHistoryNode;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IFragmentTracer;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * A fragment tracer based on a {@link IFileHistoryGraph}.
 */
public class FragmentTracer implements IFragmentTracer {

    @Override
    public List<IFragment> traceFragment(final IFragment fragment) {
        final ArrayList<IFragment> result = new ArrayList<>();
        final IRevisionedFile file = fragment.getFile();
        final IFileHistoryGraph fileHistoryGraph = file.getRepository().getFileHistoryGraph();
        final IFileHistoryNode node = fileHistoryGraph.getNodeFor(file);
        if (node != null) {
            for (final IRevisionedFile leafRevision : fileHistoryGraph.getLatestFiles(file)) {
                final IFileHistoryNode descendant = fileHistoryGraph.getNodeFor(leafRevision);
                final Set<? extends IFileDiff> fileDiffs = descendant.buildHistories(node);
                for (final IFileDiff fileDiff : fileDiffs) {
                    final IFragment lastFragment = fileDiff.traceFragment(fragment);
                    result.add(lastFragment);
                }
            }
        }

        return result;
    }

    @Override
    public List<IRevisionedFile> traceFile(final IRevisionedFile file) {
        final ArrayList<IRevisionedFile> result = new ArrayList<>();
        final IFileHistoryGraph fileHistoryGraph = file.getRepository().getFileHistoryGraph();
        final IFileHistoryNode node = fileHistoryGraph.getNodeFor(file);
        if (node != null) {
            for (final IRevisionedFile leafRevision : fileHistoryGraph.getLatestFiles(file)) {
                result.add(leafRevision);
            }
        }

        return result;
    }
}
