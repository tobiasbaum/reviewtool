package de.setsoftware.reviewtool.ordering2.base;

import java.util.Set;

public interface Pattern {

    public abstract Set<PatternMatch> patternMatches(StopRelationGraph graph);

}
