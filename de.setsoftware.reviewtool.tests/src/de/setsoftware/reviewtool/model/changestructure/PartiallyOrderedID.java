package de.setsoftware.reviewtool.model.changestructure;

import de.setsoftware.reviewtool.base.IPartiallyComparable;

final class PartiallyOrderedID implements IPartiallyComparable<PartiallyOrderedID> {
    private final String id;
    PartiallyOrderedID(final String id) {
        this.id = id;
    }

    @Override
    public boolean le(final PartiallyOrderedID other) {
        return this.id.length() < other.id.length()
                || this.id.equals(other.id);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof PartiallyOrderedID) {
            final PartiallyOrderedID other = (PartiallyOrderedID) obj;
            return this.id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
