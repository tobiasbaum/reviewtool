package de.setsoftware.reviewtool.ordering;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;

/**
 * Group stops that belong to the same system test.
 * This matcher relies on naming conventions and is therefore probably not well portable to other contexts.
 */
public class InSameSystemTestRelation implements RelationMatcher {

    private static final String SYSTEMTEST_FOLDER_NAME = "Systemtestfaelle";
    private static final String AUSGABEDATEIEN = "Ausgabedateien.expected";
    private static final String EINGABEDATEIEN = "Eingabedateien";

    private final HierarchyExplicitness explicitness;

    public InSameSystemTestRelation(HierarchyExplicitness explicitness) {
        this.explicitness = explicitness;
    }

    @Override
    public Collection<? extends OrderingInfo> determineMatches(List<ChangePart> changeParts) {
        final Multimap<String, ChangePart> grouping = new Multimap<>();
        for (final ChangePart c : changeParts) {
            final String systemTest = this.determineSystemTestFolder(c.getStops().get(0).getMostRecentFile());
            if (systemTest != null) {
                grouping.put(systemTest, c);
            }
        }

        final List<OrderingInfo> ret = new ArrayList<>();
        for (final Entry<String, List<ChangePart>> e : grouping.entrySet()) {
            ret.add(new SimpleUnorderedMatch(this.explicitness, e.getKey(), e.getValue()));
        }
        return ret;
    }

    private String determineSystemTestFolder(IRevisionedFile file) {
        final File dir = new File(file.getPath()).getParentFile();
        if (!this.isSystemTest(dir)) {
            return null;
        }
        final File testDir = this.stripOffTestSubdirs(dir);
        return testDir != null ? testDir.toString() : null;
    }

    private boolean isSystemTest(File dir) {
        if (dir == null) {
            return false;
        }
        if (dir.getName().startsWith(SYSTEMTEST_FOLDER_NAME)) {
            return true;
        }
        return this.isSystemTest(dir.getParentFile());
    }

    private File stripOffTestSubdirs(File dir) {
        File cur = dir;
        do {
            if (this.isTestSubdir(dir.getName())) {
                return dir.getParentFile();
            }
            cur = cur.getParentFile();
        } while (cur != null);
        return dir;
    }

    private boolean isTestSubdir(String name) {
        return name.equals(AUSGABEDATEIEN) || name.equals(EINGABEDATEIEN);
    }

}
