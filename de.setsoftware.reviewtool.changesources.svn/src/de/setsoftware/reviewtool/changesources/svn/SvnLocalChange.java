package de.setsoftware.reviewtool.changesources.svn;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.SortedMap;

import de.setsoftware.reviewtool.changesources.core.IScmLocalChange;
import de.setsoftware.reviewtool.model.api.ILocalRevision;
import de.setsoftware.reviewtool.model.api.IWorkingCopy;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Implements {@link IScmLocalChange} for Subversion.
 */
@SuppressWarnings("serial")
final class SvnLocalChange extends SvnChange implements IScmLocalChange<SvnChangeItem> {

    private final IWorkingCopy wc;

    /**
     * Constructor.
     *
     * @param wc The associated working copy.
     * @param changeItems The change items.
     */
    SvnLocalChange(final IWorkingCopy wc, final SortedMap<String, SvnChangeItem> changeItems) {
        super(changeItems);
        this.wc = wc;
    }

    @Override
    public IWorkingCopy getWorkingCopy() {
        return this.wc;
    }

    @Override
    public ILocalRevision toRevision() {
        return ChangestructureFactory.createLocalRevision(this.wc);
    }

    @Override
    public String toString() {
        return "WORKING";
    }

    /**
     * Objects of this class are not serializable.
     */
    private void writeObject(@SuppressWarnings("unused") final ObjectOutputStream out) throws NotSerializableException {
        throw new NotSerializableException();
    }

    /**
     * Objects of this class are not serializable.
     */
    private void readObject(@SuppressWarnings("unused") final ObjectInputStream in) throws NotSerializableException {
        throw new NotSerializableException();
    }
}
