package de.setsoftware.reviewtool.changesources.svn;

import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import de.setsoftware.reviewtool.changesources.core.IScmChangeItem;

/**
 * Implements {@link IScmChangeItem} for Subversion.
 */
final class SvnChangeItem implements IScmChangeItem {

    private static final long serialVersionUID = -3997711069754371839L;

    private final String path;
    private final long prevRevision;
    private final String copyPath;
    private final long copyRevision;
    private final char type;
    private final char kind;

    /**
     * Constructor.
     * @param logEntryPath The log entry path.
     * @param prevRevision The previous revision.
     */
    SvnChangeItem(final SVNLogEntryPath logEntryPath, final long prevRevision) {
        this.path = logEntryPath.getPath();
        this.copyPath = logEntryPath.getCopyPath();
        this.prevRevision = prevRevision;
        this.copyRevision = logEntryPath.getCopyRevision();
        this.type = logEntryPath.getType();
        this.kind = mapStatusKind(logEntryPath.getKind());
    }

    /**
     * Constructor.
     * @param repo The Subversion repository.
     * @param status The Subversion status of the change item.
     */
    SvnChangeItem(final SvnRepository repo, final SVNStatus status) {
        if (status.getRevision().equals(SVNRevision.UNDEFINED)) {
            this.prevRevision = SVNRevision.BASE.getNumber();
        } else {
            this.prevRevision = status.getRevision().getNumber();
        }

        final String copySourceUrl = status.getCopyFromURL();
        if (copySourceUrl != null) {
            this.path = '/' + status.getRepositoryRelativePath();
            this.copyPath = copySourceUrl.substring(repo.getId().length());
            this.copyRevision = status.getCopyFromRevision().getNumber();
        } else {
            this.path = '/' + status.getRepositoryRelativePath();
            this.copyPath = null;
            this.copyRevision = -1;
        }
        this.type = mapStatusTypeToLogEntryType(status.getNodeStatus());
        this.kind = mapStatusKind(status.getKind());
    }

    /**
     * Constructor. Only for testing.
     */
    SvnChangeItem(
            final String path,
            final long prevRevision,
            final String copyPath,
            final long copyRevision,
            final char type,
            final char kind) {
        this.path = path;
        this.prevRevision = prevRevision;
        this.copyPath = copyPath;
        this.copyRevision = copyRevision;
        this.type = type;
        this.kind = kind;
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

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public boolean isFile() {
        return this.kind == 'F';
    }

    @Override
    public boolean isDirectory() {
        return this.kind == 'D';
    }

    @Override
    public boolean isAdded() {
        return this.type == SVNLogEntryPath.TYPE_ADDED || this.type == SVNLogEntryPath.TYPE_REPLACED;
    }

    @Override
    public boolean isChanged() {
        return this.type == SVNLogEntryPath.TYPE_MODIFIED;
    }

    @Override
    public boolean isDeleted() {
        return this.type == SVNLogEntryPath.TYPE_DELETED || this.type == SVNLogEntryPath.TYPE_REPLACED;
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
}
