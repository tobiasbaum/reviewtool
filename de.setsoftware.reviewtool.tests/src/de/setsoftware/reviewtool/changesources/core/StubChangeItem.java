package de.setsoftware.reviewtool.changesources.core;

import de.setsoftware.reviewtool.changesources.core.IScmChangeItem;

public final class StubChangeItem implements IScmChangeItem {

    private static final long serialVersionUID = -6100863740958720060L;
    private final String path;
    private final boolean isFile;
    private final boolean isDirectory;
    private final boolean isAdded;
    private final boolean isChanged;
    private final boolean isDeleted;

    public StubChangeItem(
            final String path,
            final boolean isFile,
            final boolean isDirectory,
            final boolean isAdded,
            final boolean isChanged,
            final boolean isDeleted) {
        this.path = path;
        this.isFile = isFile;
        this.isDirectory = isDirectory;
        this.isAdded = isAdded;
        this.isChanged = isChanged;
        this.isDeleted = isDeleted;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public boolean isFile() {
        return this.isFile;
    }

    @Override
    public boolean isDirectory() {
        return this.isDirectory;
    }

    @Override
    public boolean isAdded() {
        return this.isAdded;
    }

    @Override
    public boolean isChanged() {
        return this.isChanged;
    }

    @Override
    public boolean isDeleted() {
        return this.isDeleted;
    }
}
