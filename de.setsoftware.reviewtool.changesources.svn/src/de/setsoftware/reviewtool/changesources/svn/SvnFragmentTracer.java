package de.setsoftware.reviewtool.changesources.svn;

import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;
import de.setsoftware.reviewtool.model.changestructure.FileDiff;
import de.setsoftware.reviewtool.model.changestructure.FileHistoryNode;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;

/**
 * A SVN based fragment tracer.
 */
public class SvnFragmentTracer implements IFragmentTracer {

    private final SvnFileHistoryGraph fileHistoryGraph;

    public SvnFragmentTracer(final SvnFileHistoryGraph fileHistoryGraph) {
        this.fileHistoryGraph = fileHistoryGraph;
    }

    @Override
    public List<Fragment> traceFragment(Fragment fragment) {
        final ArrayList<Fragment> result = new ArrayList<>();
        final FileHistoryNode node = this.fileHistoryGraph.getNodeFor(fragment.getFile());
        if (node != null) {
            for (final FileInRevision leafRevision : this.fileHistoryGraph.getLatestFiles(node.getFile())) {
                final FileHistoryNode descendant = this.fileHistoryGraph.getNodeFor(leafRevision);
                final FileDiff fileDiff = descendant.buildHistory(node);
                final Fragment lastFragment = fileDiff.traceFragment(fragment);
                result.add(ChangestructureFactory.createFragment(
                        descendant.getFile(),
                        lastFragment.getFrom(),
                        lastFragment.getTo(),
                        lastFragment.getContent()));
            }
        }

        return result;
    }

    @Override
    public List<FileInRevision> traceFile(FileInRevision file) {
        final ArrayList<FileInRevision> result = new ArrayList<>();
        final FileHistoryNode node = this.fileHistoryGraph.getNodeFor(file);
        if (node != null) {
            for (final FileInRevision leafRevision : this.fileHistoryGraph.getLatestFiles(file)) {
                result.add(ChangestructureFactory.createFileInRevision(
                        leafRevision.getPath(),
                        ChangestructureFactory.createLocalRevision(),
                        leafRevision.getRepository()));
            }
        }

        return result;
    }
}
