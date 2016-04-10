package de.setsoftware.reviewtool.fragmenttracers.svn;

import de.setsoftware.reviewtool.model.changestructure.FileInRevision;
import de.setsoftware.reviewtool.model.changestructure.Fragment;
import de.setsoftware.reviewtool.model.changestructure.IFragmentTracer;
import de.setsoftware.reviewtool.model.changestructure.Revision;

/**
 * A simple fragment tracer that does not trace position changes and only traces file renames.
 */
public class BasicFragmentTracer implements IFragmentTracer {

    @Override
    public Fragment getInRevision(Fragment fragment, Revision revision) {
        return new Fragment(this.getInRevision(fragment.getFile(), revision),
                fragment.getFrom(),
                fragment.getTo());
    }

    private FileInRevision getInRevision(FileInRevision file, Revision revision) {
        // TODO Umbenennungen aus SVN beachten
        return new FileInRevision(file.getPath(), revision);
    }

}
