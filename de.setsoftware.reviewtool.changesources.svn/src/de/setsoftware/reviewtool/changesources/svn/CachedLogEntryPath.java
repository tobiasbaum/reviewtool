package de.setsoftware.reviewtool.changesources.svn;

import java.io.Serializable;

import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

/**
 * Stores all relevant data from a {@link SVNLogEntryPath} or a {@link SVNStatus}.
 */
public class CachedLogEntryPath implements Serializable {

    private static final long serialVersionUID = -7052753449952234944L;

    private final String path;
    private final String copyPath;
    private final long prevRevision;
    private final char type;
    private final char kind;

    public CachedLogEntryPath(final SVNLogEntryPath value, final long prevRevision) {
        this.path = value.getPath();
        this.copyPath = value.getCopyPath();
        if (this.copyPath == null) {
            this.prevRevision = prevRevision;
        } else {
            this.prevRevision = value.getCopyRevision();
        }
        this.type = value.getType();
        this.kind = mapStatusKind(value.getKind());
    }

    private static char mapStatusKind(final SVNNodeKind nodeKind) {
        if (nodeKind.equals(SVNNodeKind.FILE)) {
            return 'F';
        } else if (nodeKind.equals(SVNNodeKind.DIR)) {
            return 'D';
        } else {
            return ' ';
        }
    }

    public CachedLogEntryPath(final SvnRepo repo, final SVNStatus status) {
        final String copySourceUrl = status.getCopyFromURL();
        if (copySourceUrl != null) {
            this.path = '/' + status.getRepositoryRelativePath();
            this.copyPath = copySourceUrl.substring(repo.getRemoteUrl().toString().length());
            this.prevRevision = status.getCopyFromRevision().getNumber();
        } else {
            this.path = '/' + status.getRepositoryRelativePath();
            this.copyPath = null;
            if (status.getRevision().equals(SVNRevision.UNDEFINED)) {
                this.prevRevision = Long.MAX_VALUE;
            } else {
                this.prevRevision = status.getRevision().getNumber();
            }
        }
        this.type = mapStatusTypeToLogEntryType(status.getNodeStatus());
        this.kind = mapStatusKind(status.getKind());
    }

    private static char mapStatusTypeToLogEntryType(final SVNStatusType type) {
        if (type == SVNStatusType.STATUS_ADDED) {
            return SVNLogEntryPath.TYPE_ADDED;
        } else if (type == SVNStatusType.STATUS_DELETED) {
            return SVNLogEntryPath.TYPE_DELETED;
        } else if (type == SVNStatusType.STATUS_MODIFIED) {
            return SVNLogEntryPath.TYPE_MODIFIED;
        } else if (type == SVNStatusType.STATUS_REPLACED) {
            return SVNLogEntryPath.TYPE_REPLACED;
        } else {
            return ' ';
        }
    }

    public String getPath() {
        return this.path;
    }

    public String getCopyPath() {
        return this.copyPath;
    }

    public long getAncestorRevision() {
        return this.prevRevision;
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
