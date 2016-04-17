package de.setsoftware.reviewtool.slicingalgorithms;

import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.base.ValueWrapper;
import de.setsoftware.reviewtool.model.changestructure.BinaryChange;
import de.setsoftware.reviewtool.model.changestructure.Change;
import de.setsoftware.reviewtool.model.changestructure.ChangeVisitor;
import de.setsoftware.reviewtool.model.changestructure.Commit;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;
import de.setsoftware.reviewtool.model.changestructure.ISlicingStrategy;
import de.setsoftware.reviewtool.model.changestructure.LocalRevision;
import de.setsoftware.reviewtool.model.changestructure.Slice;
import de.setsoftware.reviewtool.model.changestructure.SliceFragment;
import de.setsoftware.reviewtool.model.changestructure.TextualChange;

/**
 * Simple slicing algorithm that creates one slice per commit.
 */
public class OneSlicePerCommit implements ISlicingStrategy {

    private final IFragmentTracer tracer;

    public OneSlicePerCommit(IFragmentTracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public List<Slice> toSlices(List<Commit> changes) {
        final List<Slice> ret = new ArrayList<>();
        for (final Commit c : changes) {
            ret.add(new Slice(
                    c.getMessage(),
                    this.toSliceFragments(c.getChanges())));
        }
        return ret;
    }

    private List<SliceFragment> toSliceFragments(List<Change> changes) {
        final List<SliceFragment> ret = new ArrayList<>();
        for (final Change c : changes) {
            ret.add(this.toSliceFragment(c));
        }
        return ret;
    }

    private SliceFragment toSliceFragment(Change c) {
        final ValueWrapper<SliceFragment> ret = new ValueWrapper<>();
        c.accept(new ChangeVisitor() {

            @Override
            public void handle(TextualChange visitee) {
                ret.setValue(new SliceFragment(
                        visitee.getFrom(),
                        visitee.getTo(),
                        OneSlicePerCommit.this.tracer.traceFragment(visitee.getTo(), new LocalRevision())));
            }

            @Override
            public void handle(BinaryChange visitee) {
                ret.setValue(new SliceFragment(
                        visitee.getFrom(),
                        visitee.getTo(),
                        OneSlicePerCommit.this.tracer.traceFile(visitee.getTo(), new LocalRevision())));
            }

        });
        return ret.get();
    }

}
