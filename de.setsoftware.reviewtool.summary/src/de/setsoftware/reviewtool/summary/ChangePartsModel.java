package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.setsoftware.reviewtool.summary.ChangePart.Kind;

class ChangePart {
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

class ChangeParts {
    List<ChangePart> methods = new ArrayList<>();
    List<ChangePart> files = new ArrayList<>();
    List<ChangePart> types = new ArrayList<>();

    public void addPart(ChangePart part) {
        if (part.getType().equals(Kind.METHOD)) {
            if (!methods.contains(part)) {
                methods.add(part);
            }
        }
        if (part.getType().equals(Kind.NON_SOURCE_FILE)) {
            if (!files.contains(part)) {
                files.add(part);
            }
        }
        if (part.getType().equals(Kind.TYPE)) {
            if (!types.contains(part)) {
                types.add(part);
            }
        }
    }

    public void removePart(ChangePart part) {
        if (part.getType().equals(Kind.METHOD)) {
            methods.remove(part);
        }
        if (part.getType().equals(Kind.NON_SOURCE_FILE)) {
            files.remove(part);
        }
        if (part.getType().equals(Kind.TYPE)) {
            types.remove(part);
        }
    }

    public void sort() {
        Collections.sort(types, new Comparator<ChangePart>() {
            @Override
            public int compare(ChangePart part1, ChangePart part2) {
                if (part2.relevance - part1.relevance == 0) {
                    (part1.getParent() + part1.getName()).compareTo((part2.getParent() + part2.getName()));
                }
                return part2.relevance - part1.relevance;
            }
        });
        Collections.sort(methods, new Comparator<ChangePart>() {
            @Override
            public int compare(ChangePart part1, ChangePart part2) {
                if (part2.relevance - part1.relevance == 0) {
                    (part1.getParent() + part1.getName()).compareTo((part2.getParent() + part2.getName()));
                }
                return part2.relevance - part1.relevance;
            }
        });
        Collections.sort(files, new Comparator<ChangePart>() {
            @Override
            public int compare(ChangePart part1, ChangePart part2) {
                return (part1.getParent() + part1.getName()).compareTo((part2.getParent() + part2.getName()));
            }
        });
    }
}

/**
 * Default representation of a commits that is used, if other summary techniques
 * failed or misses a change part. It consists of basic parts such a methods,
 * types or files, classified by action such addition, deletion etc. Change
 * parts can be sorted by some relevance score defined by controller. Other
 * summarize techniques can remove change parts from model to prevent
 * redundancies in generated summary.
 */
public class ChangePartsModel {
    ChangeParts newParts = new ChangeParts();
    ChangeParts deletedParts = new ChangeParts();
    ChangeParts changedParts = new ChangeParts();

    public void sort() {
        newParts.sort();
        deletedParts.sort();
        changedParts.sort();
    }
}
