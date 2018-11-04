package de.setsoftware.reviewtool.summary;

/**
 * Single part of a change such a method, type or file.
 */
class ChangePart {
    /**
     * Kind of change part.
     */
    public enum Kind {
        TYPE, ATTRIBUTE, METHOD, NON_SOURCE_FILE;
    }

    private final String name;
    private final String parent;
    private final Kind type;

    public int relevance = 0;

    public ChangePart(String name, String parent, Kind type) {
        this.name = name;
        this.parent = parent;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public String getParent() {
        return this.parent;
    }

    public Kind getType() {
        return this.type;
    }

    @Override
    public String toString() {
        if (this.type == Kind.NON_SOURCE_FILE) {
            return this.name;
        } else {
            return this.name + " in " + this.parent;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.parent == null) ? 0 : this.parent.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final ChangePart other = (ChangePart) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!this.parent.equals(other.parent)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }

    public TextWithStyles toStyledText() {
        if (this.type == Kind.NON_SOURCE_FILE) {
            return new TextWithStyles().addNormal(this.name);
        } else {
            return new TextWithStyles().addNormal(this.name).addGray(" in " + this.parent);
        }
    }
}