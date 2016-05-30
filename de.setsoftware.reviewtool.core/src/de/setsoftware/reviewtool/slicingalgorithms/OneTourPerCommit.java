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
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.TextualChangeHunk;
import de.setsoftware.reviewtool.model.changestructure.Tour;

/**
 * Simple slicing algorithm that creates one tour per commit, without looking
 * at the order of stops.
 */
public class OneTourPerCommit implements ISlicingStrategy {

    private final IFragmentTracer tracer;

    public OneTourPerCommit(IFragmentTracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public List<Tour> toTours(List<Commit> changes) {
        final List<Tour> ret = new ArrayList<>();
        for (final Commit c : changes) {
            ret.add(new Tour(
                    c.getMessage(),
                    this.toSliceFragments(c.getChanges())));
        }
        return ret;
    }

    private List<Stop> toSliceFragments(List<Change> changes) {
        final List<Stop> ret = new ArrayList<>();
        for (final Change c : changes) {
            ret.add(this.toSliceFragment(c));
        }
        return ret;
    }

    private Stop toSliceFragment(Change c) {
        final ValueWrapper<Stop> ret = new ValueWrapper<>();
        c.accept(new ChangeVisitor() {

            @Override
            public void handle(TextualChangeHunk visitee) {
                ret.setValue(new Stop(
                        visitee.getFrom(),
                        visitee.getTo(),
                        OneTourPerCommit.this.tracer.traceFragment(visitee.getTo(), new LocalRevision())));
            }

            @Override
            public void handle(BinaryChange visitee) {
                ret.setValue(new Stop(
                        visitee.getFrom(),
                        visitee.getTo(),
                        OneTourPerCommit.this.tracer.traceFile(visitee.getTo(), new LocalRevision())));
            }

        });
        return ret.get();
    }

}
