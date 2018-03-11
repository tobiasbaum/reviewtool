package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.setsoftware.reviewtool.summary.ChangePart.Kind;

/**
 * List of changes that consists of basic parts such a methods, types or files.
 */
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

    /**
     * Sort parts using relevance or alphabetic, if relevance is same.
     */
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