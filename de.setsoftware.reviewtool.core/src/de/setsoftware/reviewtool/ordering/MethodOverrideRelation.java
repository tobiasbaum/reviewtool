package de.setsoftware.reviewtool.ordering;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import de.setsoftware.reviewtool.base.Multimap;

/**
 * Relation that groups a method that is overridden with its overriding methods.
 * Relies on the Override annotation and is not 100% accurate.
 */
public class MethodOverrideRelation extends JavaParserBasedRelation {

    public MethodOverrideRelation(HierarchyExplicitness explicitness) {
        super(explicitness);
    }

    @Override
    protected void handleType(final Multimap<MethodKey, ChangePart> centerCandidates,
            final Multimap<MethodKey, ChangePart> rayCandidates, final LineRangeMap lineRanges,
            final TypeDeclaration<?> t) {

        for (final BodyDeclaration<?> d : t.getMembers()) {
            if (d instanceof MethodDeclaration) {
                final MethodDeclaration m = (MethodDeclaration) d;
                if (canBeRay(m, t)) {
                    rayCandidates.putAll(new MethodKey(m), lineRanges.getOverlappingParts(m));
                } else if (canBeCenter(m, t)) {
                    centerCandidates.putAll(new MethodKey(m), lineRanges.getOverlappingParts(m));
                }

            } else if (d instanceof TypeDeclaration) {
                this.handleType(centerCandidates, rayCandidates, lineRanges, (TypeDeclaration<?>) d);
            }
        }
    }

    private static boolean canBeCenter(MethodDeclaration m, TypeDeclaration<?> t) {
        return !m.isPrivate() && !m.isFinal() && !m.isStatic() && !isFinal(t);
    }

    private static boolean isFinal(TypeDeclaration<?> t) {
        for (final Modifier m : t.getModifiers()) {
            if (m.equals(Modifier.finalModifier())) {
                return true;
            }
        }
        return false;
    }

    private static boolean canBeRay(MethodDeclaration m, TypeDeclaration<?> t) {
        return hasOverride(m);
    }

    private static boolean hasOverride(MethodDeclaration m) {
        for (final AnnotationExpr a : m.getAnnotations()) {
            if (a.getNameAsString().equals("Override")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String toRelationKey(MethodKey key) {
        return key.toString() + " hierarchy";
    }

}
