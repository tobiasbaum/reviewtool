package de.setsoftware.reviewtool.ordering;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import de.setsoftware.reviewtool.base.Logger;
import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IStop;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculator;
import de.setsoftware.reviewtool.ordering.efficientalgorithm.TourCalculatorControl;

/**
 * Superclass for relations that work on parsed Java code.
 */
public abstract class JavaParserBasedRelation implements RelationMatcher {

    private final HierarchyExplicitness explicitness;

    public JavaParserBasedRelation(HierarchyExplicitness explicitness) {
        this.explicitness = explicitness;
    }

    @Override
    public Collection<? extends OrderingInfo> determineMatches(List<ChangePart> changeParts,
            TourCalculatorControl control) throws InterruptedException {

        final Multimap<IRevisionedFile, ChangePart> groupedByFile = new Multimap<>();
        for (final ChangePart c : changeParts) {
            if (c.isFullyIrrelevantForReview()) {
                continue;
            }
            //all stops of a change part should be in the same file, so the first is sufficient
            final IStop stop = c.getStops().get(0);
            if (stop.isBinaryChange()) {
                continue;
            }
            final IRevisionedFile file = stop.getMostRecentFile();
            if (!file.getPath().endsWith(".java")) {
                continue;
            }
            groupedByFile.put(file, c);
        }

        final Multimap<MethodKey, ChangePart> centerCandidates = new Multimap<>();
        final Multimap<MethodKey, ChangePart> rayCandidates = new Multimap<>();
        for (final Entry<IRevisionedFile, List<ChangePart>> e : groupedByFile.entrySet()) {
            TourCalculator.checkInterruption(control);

            try {
                final byte[] fileContent = e.getKey().getContents();
                if (fileContent == null) {
                    continue;
                }
                final LineRangeMap lineRanges = new LineRangeMap(e.getValue());
                final CompilationUnit c = JavaParser.parse(new ByteArrayInputStream(fileContent));
                for (final TypeDeclaration<?> t : c.getTypes()) {
                    this.handleType(centerCandidates, rayCandidates, lineRanges, t);
                }
            } catch (final Exception e1) {
                //ignore problem in files and just move on to the next
                Logger.info("Problem while parsing file " + e.getKey() + ": " + e1);
            }
        }

        final List<OrderingInfo> ret = new ArrayList<>();
        for (final Entry<MethodKey, List<ChangePart>> e : centerCandidates.entrySet()) {
            if (e.getValue().size() > 1) {
                //more than one center => something went wrong => ignore
                continue;
            }
            final List<ChangePart> rest = rayCandidates.get(e.getKey());
            if (rest.isEmpty()) {
                //not overridden => ignore
                continue;
            }
            ret.add(OrderingInfoImpl.star(
                    this.explicitness,
                    this.toRelationKey(e.getKey()),
                    e.getValue().get(0),
                    rest));
        }
        return ret;
    }

    protected abstract String toRelationKey(MethodKey key);

    protected abstract void handleType(final Multimap<MethodKey, ChangePart> centerCandidates,
            final Multimap<MethodKey, ChangePart> rayCandidates, final LineRangeMap lineRanges,
            final TypeDeclaration<?> t);

}
