package de.setsoftware.reviewtool.changesources.svn;

import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;
import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 * A simple svn based fragment tracer that does not trace position changes and only traces file renames.
 */
public class SvnFragmentTracer implements IFragmentTracer {

    @Override
    public Fragment traceFragment(Fragment fragment, Revision revision) {
        //TODO implementieren
        return new Fragment(
                this.traceFile(fragment.getFile(), revision),
                fragment.getFrom(),
                fragment.getTo(),
                fragment.getContent());
    }

    @Override
    public FileInRevision traceFile(FileInRevision file, Revision revision) {
        //TODO implementieren
        return new FileInRevision(file.getPath(), revision, file.getRepository());
    }

}
