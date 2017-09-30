package de.setsoftware.reviewtool.ordering2.base;

public final class Relation {

    private final RelationType type;
    private final String label;
    private final Stop from;
    private final Stop to;

    public Relation(RelationType type, String label, Stop from, Stop to) {
        this.type = type;
        this.label = label;
        this.from = from;
        this.to = to;
    }

    public RelationType getType() {
        return this.type;
    }

    public String getLabel() {
        return this.label;
    }

    public Stop getSourceStop() {
        return this.from;
    }

    public Stop getTargetStop() {
        return this.to;
    }

}
