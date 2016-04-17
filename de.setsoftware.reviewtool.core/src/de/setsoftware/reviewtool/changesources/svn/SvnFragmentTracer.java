package de.setsoftware.reviewtool.changesources.svn;

import de.setsoftware.reviewtool.model.changestructure.FileFragment;
import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;
import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 * A simple svn based fragment tracer that does not trace position changes and only traces file renames.
 */
public class SvnFragmentTracer implements IFragmentTracer {

    @Override
    public FileFragment traceFragment(FileFragment fragment, Revision revision) {
        //TODO implementieren
        return fragment;
    }

    @Override
    public FileInRevision traceFile(FileInRevision file, Revision revision) {
        //TODO implementieren
        return file;
    }

}
