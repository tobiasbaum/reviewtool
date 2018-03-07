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

    private String name;
    private String parent;
    private Kind type;

    public int relevance = 0;

    public ChangePart(String name, String parent, Kind type) {
        this.name = name;
        this.parent = parent;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public Kind getType() {
        return type;
    }

    @Override
    public String toString() {
        if (type == Kind.NON_SOURCE_FILE) {
            return name;
        } else {
            return name + " in " + parent;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        ChangePart other = (ChangePart) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!parent.equals(other.parent)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }
}