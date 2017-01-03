package de.setsoftware.reviewtool.ordering;

public class RelatednessReason {

    private final RelatednessReasonType type;
    private final String id;


    public RelatednessReason(RelatednessReasonType type, String id) {
        this.type = type;
        this.id = id;
    }

    public RelatednessReasonType getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() + this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RelatednessReason)) {
            return false;
        }
        final RelatednessReason r = (RelatednessReason) o;
        return this.id.equals(r.id) && this.type.equals(r.type);
    }

    @Override
    public String toString() {
        return this.type + "-" + this.id;
    }

}
