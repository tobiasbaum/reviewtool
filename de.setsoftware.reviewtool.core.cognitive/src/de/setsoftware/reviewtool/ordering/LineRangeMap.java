package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.nodeTypes.NodeWithRange;

import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * Helper class to check whether Java AST nodes overlap with {@link ChangePart}s.
 */
class LineRangeMap {

    private final List<ChangePart> parts;

    public LineRangeMap(List<ChangePart> parts) {
        this.parts = parts;
    }

    public List<ChangePart> getOverlappingParts(NodeWithRange<?> m) {
        final List<ChangePart> ret = new ArrayList<>();
        for (final ChangePart p : this.parts) {
            if (this.overlaps(p, m)) {
                ret.add(p);
            }
        }
        return ret;
    }

    private boolean overlaps(ChangePart p, NodeWithRange<?> m) {
        for (final Stop s : p.getStops()) {
            if (this.overlaps(s, m)) {
                return true;
            }
        }
        return false;
    }

    private boolean overlaps(Stop s, NodeWithRange<?> m) {
        final IFragment fragment = s.getOriginalMostRecentFragment();
        final int startLineS = fragment.getFrom().getLine();
        final int endLineS = fragment.getTo().getLine() - (fragment.getTo().getColumn() > 1 ? 0 : 1);
        final int startLineM = m.getBegin().get().line;
        final int endLineM = m.getEnd().get().line;
        return (endLineS >= startLineM) && (endLineM >= startLineS);
    }

}
