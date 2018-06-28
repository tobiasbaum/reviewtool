package de.setsoftware.reviewtool.changesources.core;

import java.util.Map;

import de.setsoftware.reviewtool.changesources.core.IScmLocalChange;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

public final class StubLocalChange implements IScmLocalChange<StubChangeItem> {

    private final IWorkingCopy wc;
    private final Map<String, StubChangeItem> changeItems;

    public StubLocalChange(final IWorkingCopy wc, final Map<String, StubChangeItem> changeItems) {
        this.wc = wc;
        this.changeItems = changeItems;
    }

    @Override
    public Map<String, StubChangeItem> getChangedItems() {
        return this.changeItems;
    }

    @Override
    public void integrateInto(final IMutableFileHistoryGraph graph) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IWorkingCopy getWorkingCopy() {
        return this.wc;
    }

    @Override
    public ILocalRevision toRevision() {
        return ChangestructureFactory.createLocalRevision(this.wc);
    }
}
