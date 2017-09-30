package de.setsoftware.reviewtool.ordering.efficientalgorithm;

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

    @Override
    public int hashCode() {
        return this.matchSet.hashCode() + this.distinguishedPart.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PositionRequest)) {
            return false;
        }
        final PositionRequest<?> other = (PositionRequest<?>) o;
        return other.matchSet.equals(this.matchSet)
            && other.distinguishedPart.equals(this.distinguishedPart)
            && other.targetPosition == this.targetPosition;
    }

}
