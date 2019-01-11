package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import de.setsoftware.reviewtool.base.Multimap;
import de.setsoftware.reviewtool.summary.ChangePart.Kind;

/**
 * List of changes that consists of basic parts such a methods, types or files.
 */
class ChangeParts {
    Multimap<String, ChangePart> methods = new Multimap<>();
    List<ChangePart> files = new ArrayList<>();
    Multimap<String, ChangePart> types = new Multimap<>();

    public void addPart(ChangePart part) {
        if (part.getType().equals(Kind.METHOD)) {
            if (!this.methods.get(part.getSourceFolder()).contains(part)) {
                this.methods.put(part.getSourceFolder(), part);
            }
        }
        if (part.getType().equals(Kind.NON_SOURCE_FILE)) {
            if (!this.files.contains(part)) {
                this.files.add(part);
            }
        }
        if (part.getType().equals(Kind.TYPE)) {
            if (!this.types.get(part.getSourceFolder()).contains(part)) {
                this.types.put(part.getSourceFolder(), part);
            }
        }
    }

    public void removePart(ChangePart part) {
        if (part.getType().equals(Kind.METHOD)) {
            this.methods.removeValue(part.getSourceFolder(), part);
        }
        if (part.getType().equals(Kind.NON_SOURCE_FILE)) {
            this.files.remove(part);
        }
        if (part.getType().equals(Kind.TYPE)) {
            this.types.removeValue(part.getSourceFolder(), part);
        }
    }

    /**
     * Sort parts using relevance or alphabetic, if relevance is same.
     */
    public void sort() {
        this.types.sortValues(new Comparator<ChangePart>() {
            @Override
            public int compare(ChangePart part1, ChangePart part2) {
                if (part2.relevance - part1.relevance == 0) {
                    (part1.getParent() + part1.getName()).compareTo((part2.getParent() + part2.getName()));
                }
                return part2.relevance - part1.relevance;
            }
        });
        this.methods.sortValues(new Comparator<ChangePart>() {
            @Override
            public int compare(ChangePart part1, ChangePart part2) {
                if (part2.relevance - part1.relevance == 0) {
                    (part1.getParent() + part1.getName()).compareTo((part2.getParent() + part2.getName()));
                }
                return part2.relevance - part1.relevance;
            }
        });
        Collections.sort(this.files, new Comparator<ChangePart>() {
            @Override
            public int compare(ChangePart part1, ChangePart part2) {
                return (part1.getParent() + part1.getName()).compareTo((part2.getParent() + part2.getName()));
            }
        });
    }

    public List<ChangePart> getAllMethodParts() {
        final List<ChangePart> ret = new ArrayList<>();
        for (final Entry<String, List<ChangePart>> e : this.methods.entrySet()) {
            ret.addAll(e.getValue());
        }
        return ret;
    }

    public List<ChangePart> getAllTypeParts() {
        final List<ChangePart> ret = new ArrayList<>();
        for (final Entry<String, List<ChangePart>> e : this.types.entrySet()) {
            ret.addAll(e.getValue());
        }
        return ret;
    }

    /**
     * If the current object contains a part equal to the given one, the relevance of the contained part
     * is increased by one.
     */
    public void increaseRelevance(ChangePart part) {
        int idx;
        switch (part.getType()) {
        case TYPE:
            idx = this.types.get(part.getSourceFolder()).indexOf(part);
            if (idx >= 0) {
                this.types.get(part.getSourceFolder()).get(idx).relevance++;
            }
            break;
        case METHOD:
            idx = this.methods.get(part.getSourceFolder()).indexOf(part);
            if (idx >= 0) {
                this.methods.get(part.getSourceFolder()).get(idx).relevance++;
            }
            break;
        case NON_SOURCE_FILE:
            idx = this.files.indexOf(part);
            if (idx >= 0) {
                this.files.get(idx).relevance++;
            }
            break;
        case ATTRIBUTE:
        default:
            break;
        }
    }

    /**
     * Returns true iff this object contains a part equal to the given one.
     */
    public boolean contains(ChangePart part) {
        switch (part.getType()) {
        case TYPE:
            return this.types.get(part.getSourceFolder()).contains(part);
        case METHOD:
            return this.methods.get(part.getSourceFolder()).contains(part);
        case NON_SOURCE_FILE:
            return this.files.contains(part);
        case ATTRIBUTE:
        default:
            return false;
        }
    }
}