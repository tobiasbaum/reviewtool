package de.setsoftware.reviewtool.ordering2.efficientalgorithm;

public class PositionRequest<T> {

    private final MatchSet<T> matchSet;
    private final T distinguishedPart;
    private final TargetPosition targetPosition;

    public PositionRequest(MatchSet<T> matchSet, T distinguishedPart, TargetPosition pos) {
        this.matchSet = matchSet;
        this.distinguishedPart = distinguishedPart;
        this.targetPosition = pos;
    }

    public MatchSet<T> getMatchSet() {
        return this.matchSet;
    }

    public T getDistinguishedPart() {
        return this.distinguishedPart;
    }

    public TargetPosition getTargetPosition() {
        return this.targetPosition;
    }

}
