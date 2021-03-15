package de.setsoftware.reviewtool.ordering;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import de.setsoftware.reviewtool.base.Multimap;

/**
 * Relation between methods and their callees.
 * Uses heuristics to avoid full symbol solving and is therefore not 100% accurate.
 */
public class MethodCallRelation extends JavaParserBasedRelation {

    public MethodCallRelation(HierarchyExplicitness explicitness) {
        super(explicitness);
    }

    @Override
    protected void handleType(
            Multimap<MethodKey, ChangePart> centerCandidates,
            Multimap<MethodKey, ChangePart> rayCandidates,
            LineRangeMap lineRanges,
            TypeDeclaration<?> t) {

        for (final MethodDeclaration m : t.findAll(MethodDeclaration.class)) {
            if (!this.isBlacklisted(m.getNameAsString())) {
                centerCandidates.putAll(new MethodKey(m), lineRanges.getOverlappingParts(m));
            }
        }
        for (final MethodCallExpr c : t.findAll(MethodCallExpr.class)) {
            if (!this.isBlacklisted(c.getNameAsString())) {
                rayCandidates.putAll(new MethodKey(c), lineRanges.getOverlappingParts(c));
            }
        }
    }

    private boolean isBlacklisted(String name) {
        return name.equals("equals")
            || name.equals("toString")
            || name.equals("hashCode")
            || name.equals("add");
    }

    @Override
    protected String toRelationKey(MethodKey key) {
        return key.toString() + " calls";
    }

}
