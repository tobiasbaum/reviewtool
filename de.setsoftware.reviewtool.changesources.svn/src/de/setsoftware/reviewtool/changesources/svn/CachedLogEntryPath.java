package de.setsoftware.reviewtool.changesources.svn;

import java.io.File;
import java.io.Serializable;

import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

/**
 * Stores all relevant data from a {@link SVNLogEntryPath} or a {@link SVNStatus}.
 */
final class CachedLogEntryPath implements Serializable {

    private static final long serialVersionUID = -7052753449952234943L;

    private final String path;
    private final File localPath;
    private final long prevRevision;
    private final String copyPath;
    private final long copyRevision;
    private final char type;
    private final char kind;

    CachedLogEntryPath(final SVNLogEntryPath value, final long prevRevision) {
        this.path = value.getPath();
        this.localPath = null;
        this.copyPath = value.getCopyPath();
        this.prevRevision = prevRevision;
        this.copyRevision = value.getCopyRevision();
        this.type = value.getType();
        this.kind = mapStatusKind(value.getKind());
    }

    CachedLogEntryPath(final SvnRepo repo, final SVNStatus status) {
        if (status.getRevision().equals(SVNRevision.UNDEFINED)) {
            this.prevRevision = SVNRevision.BASE.getNumber();
        } else {
            this.prevRevision = status.getCommittedRevision().getNumber();
        }

        final String copySourceUrl = status.getCopyFromURL();
        if (copySourceUrl != null) {
            this.path = '/' + status.getRepositoryRelativePath();
            this.copyPath = copySourceUrl.substring(repo.getRemoteUrl().toString().length());
            this.copyRevision = status.getCopyFromRevision().getNumber();
        } else {
            this.path = '/' + status.getRepositoryRelativePath();
            this.copyPath = null;
            this.copyRevision = -1;
        }
        this.localPath = status.getFile();
        this.type = mapStatusTypeToLogEntryType(status.getNodeStatus());
        this.kind = mapStatusKind(status.getKind());
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

    String getPath() {
        return this.path;
    }

    File getLocalPath() {
        return this.localPath;
    }

    long getAncestorRevision() {
        return this.prevRevision;
    }

    String getCopyPath() {
        return this.copyPath;
    }

    long getCopyRevision() {
        return this.copyRevision;
    }

    boolean isFile() {
        return this.kind == 'F';
    }

    boolean isDir() {
        return this.kind == 'D';
    }

    boolean isNew() {
        return this.type == SVNLogEntryPath.TYPE_ADDED;
    }

    boolean isDeleted() {
        return this.type == SVNLogEntryPath.TYPE_DELETED;
    }

    boolean isReplaced() {
        return this.type == SVNLogEntryPath.TYPE_REPLACED;
    }

}
