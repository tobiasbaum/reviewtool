package de.setsoftware.reviewtool.model.api;

import java.io.File;

public interface ICortPath {

    public abstract File toFile();

    public abstract String[] segments();

    public abstract String lastSegment();

    public abstract ICortPath removeLastSegments(int i);

    public abstract ICortPath append(String childName);

    public abstract boolean isEmpty();

    public abstract String segment(int i);

}
