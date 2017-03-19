package de.setsoftware.reviewtool.changesources.svn;

import java.io.Serializable;

import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;

/**
 * Stores all relevant data from a log entry path.
 */
public class CachedLogEntryPath implements Serializable {

    private static final long serialVersionUID = -7052753449952234945L;

    private final String path;
    private final String copyPath;
    private final long copyRevision;
    private final char type;
    private final char kind;

    public CachedLogEntryPath(SVNLogEntryPath value) {
        this.path = value.getPath();
        this.copyPath = value.getCopyPath();
        this.copyRevision = value.getCopyRevision();
        this.type = value.getType();
        if (value.getKind().equals(SVNNodeKind.FILE)) {
            this.kind = 'F';
        } else if (value.getKind().equals(SVNNodeKind.DIR)) {
            this.kind = 'D';
        } else {
            this.kind = ' ';
        }
    }

    public String getPath() {
        return this.path;
    }

    public String getCopyPath() {
        return this.copyPath;
    }

    public long getCopyRevision() {
        return this.copyRevision;
    }

    public boolean isFile() {
        return this.kind == 'F';
    }

    public boolean isDir() {
        return this.kind == 'D';
    }

    public boolean isNew() {
        return this.type == SVNLogEntryPath.TYPE_ADDED || this.type == SVNLogEntryPath.TYPE_REPLACED;
    }

    public boolean isDeleted() {
        return this.type == SVNLogEntryPath.TYPE_DELETED;
    }

}
