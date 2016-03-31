package de.setsoftware.reviewtool.slicesources.svn;

import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.ISliceSource;
import de.setsoftware.reviewtool.model.changestructure.Slice;

/**
 * A simple slice source that makes every commit a slice and every continous change segment a fragment.
 */
public class SvnSliceSource implements ISliceSource {

    @Override
    public List<Slice> getSlices(String key) {
        return null;
    }

}
