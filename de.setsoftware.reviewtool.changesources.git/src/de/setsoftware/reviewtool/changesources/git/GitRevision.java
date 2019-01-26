package de.setsoftware.reviewtool.changesources.git;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.NullOutputStream;

import de.setsoftware.reviewtool.model.api.IMutableFileHistoryGraph;
import de.setsoftware.reviewtool.model.api.IRevision;
import de.setsoftware.reviewtool.model.changestructure.ChangestructureFactory;

/**
 * Represents a Git revision.
 */
class GitRevision {

    private final GitWorkingCopy wc;
    private final RevCommit commit;

    GitRevision(GitWorkingCopy wc, RevCommit commit) {
        this.wc = wc;
        this.commit = commit;
    }

    /**
     * Returns the associated revision number as a readable string.
     */
    public String getRevisionString() {
        return this.commit.getId().name();
    }

    /**
     * Returns the {@link IRevision} for this Git commit.
     */
    public IRevision toRevision() {
        return ChangestructureFactory.createRepoRevision(new RevisionId(this.commit), this.wc.getRepository());
    }

    /**
     * Returns the associated commit date (specifically: the author date).
     */
    public Date getDate() {
        return this.commit.getAuthorIdent().getWhen();
    }

    /**
     * Returns the associated commit author.
     */
    public String getAuthor() {
        return this.commit.getAuthorIdent().getName();
    }

    /**
     * Returns the associated commit message.
     */
    public String getMessage() {
        return this.commit.getFullMessage();
    }

    /**
     * Returns the associated commit paths (path in the new revisions,
     * except for deletions, where it is the path in the old revision).
     */
    public Set<String> getChangedPaths() throws IncorrectObjectTypeException, IOException {
        final Set<String> ret = new LinkedHashSet<>();

        final Repository repository = this.wc.getRepository().getRepository();
        final RevCommit[] parents = this.commit.getParents();
        ObjectId parentTree;
        if (parents.length > 1) {
            //skip merge commits, they are usually not significant for what was changed in a review
            return Collections.emptySet();
        } else if (parents.length == 1) {
            if (parents[0].getTree() == null) {
                //sometimes, the parent objects are incomplete and the tree has to be loaded explicitly
                parentTree = repository.parseCommit(parents[0]).getTree();
            } else {
                parentTree = parents[0].getTree().getId();
            }
        } else {
            //initial commit => try to diff with empty tree
            parentTree = ObjectId.fromString("4b825dc642cb6eb9a060e54bf8d69288fbee4904");
        }

        try (final ObjectReader objectReader = repository.newObjectReader();
                final DiffFormatter diff = new DiffFormatter(NullOutputStream.INSTANCE)) {

            final CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(objectReader, parentTree);
            final CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(objectReader, this.commit.getTree());

            diff.setRepository(repository);
            diff.setDetectRenames(true);
            for (final DiffEntry entry : diff.scan(oldTreeIter, newTreeIter)) {
                if (entry.getNewPath().equals("/dev/null")) {
                    ret.add(entry.getOldPath());
                } else {
                    ret.add(entry.getNewPath());
                }
            }
        }
        return ret;
    }

    public void analyzeRevision(IMutableFileHistoryGraph graph) throws IOException {
        final Repository repository = this.wc.getRepository().getRepository();
        final RevCommit[] parents = this.commit.getParents();
        RevCommit parentId;
        ObjectId parentTree;
        if (parents.length > 1) {
            //skip merge commits, they are usually not significant for what was changed in a review
            return;
        } else if (parents.length == 1) {
            if (parents[0].getTree() == null) {
                //sometimes, the parent objects are incomplete and the tree has to be loaded explicitly
                parentId = repository.parseCommit(parents[0]);
                parentTree = parentId.getTree();
            } else {
                parentId = parents[0];
                parentTree = parents[0].getTree().getId();
            }
        } else {
            //initial commit => try to diff with empty tree
            parentTree = ObjectId.fromString("4b825dc642cb6eb9a060e54bf8d69288fbee4904");
            //this is technically not correct, but should not matter
            parentId = this.commit;
        }

        //TEST
        if (this.commit.getName().equals("90013989c2cda6517a7ad0842406a01fc7e78bdc")) {
            System.out.println("asdf");
        }

        try (final ObjectReader objectReader = repository.newObjectReader();
                final DiffFormatter diff = new DiffFormatter(NullOutputStream.INSTANCE)) {

            final CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(objectReader, parentTree);
            final CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(objectReader, this.commit.getTree());

            diff.setRepository(repository);
            diff.setDetectRenames(true);
            Map<ObjectId, String> contentToPathMap = null;
            final IRevision iRev = this.toRevision();
            for (final DiffEntry entry : diff.scan(oldTreeIter, newTreeIter)) {
                switch (entry.getChangeType()) {
                case ADD:
                    //copy detection is not that easy to get with JGit, seems that one needs to partly self code it.
                    //  We only detect exact copies, because anything else is prohibitively expensive.
                    if (contentToPathMap == null) {
                        contentToPathMap = this.determineContentToPathMap(repository, parentTree);
                    }
                    final String copyFromPath = contentToPathMap.get(entry.getNewId().toObjectId());
                    if (copyFromPath != null) {
                        graph.addCopy(copyFromPath, this.toIRevision(parentId), entry.getNewPath(), iRev);
                    } else {
                        graph.addAddition(entry.getNewPath(), iRev);
                    }
                    break;
                case DELETE:
                    //first register a change, because addDeletion does not allow the previous revision
                    //  to be specified
                    graph.addChange(entry.getOldPath(), iRev, Collections.singleton(this.toIRevision(parentId)));
                    graph.addDeletion(entry.getOldPath(), iRev);
                    break;
                case COPY:
                    graph.addCopy(entry.getOldPath(), this.toIRevision(parentId), entry.getNewPath(), iRev);
                    break;
                case RENAME:
                    graph.addDeletion(entry.getOldPath(), iRev);
                    graph.addCopy(entry.getOldPath(), this.toIRevision(parentId), entry.getNewPath(), iRev);
                    break;
                case MODIFY:
                    graph.addChange(entry.getNewPath(), iRev, Collections.singleton(this.toIRevision(parentId)));
                    break;
                default:
                    throw new AssertionError("unexpected: " + entry.getChangeType());
                }
            }
        }
    }

    private Map<ObjectId, String> determineContentToPathMap(Repository repo, ObjectId tree) throws IOException {
        try (final TreeWalk treeWalk = new TreeWalk(repo)) {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            final Map<ObjectId, String> ret = new HashMap<>();
            while (treeWalk.next()) {
                ret.put(treeWalk.getObjectId(0), treeWalk.getPathString());
            }
            return ret;
        }
    }

    private IRevision toIRevision(RevCommit parentId) {
        return ChangestructureFactory.createRepoRevision(new RevisionId(parentId.getName(), parentId.getCommitTime()),
                this.wc.getRepository());
    }

    /**
     * Returns a pretty description of this revision.
     */
    public String toPrettyString() {
        final StringBuilder sb = new StringBuilder();
        final String message = this.getMessage();
        if (!message.isEmpty()) {
            sb.append(message);
            sb.append(" ");
        }
        sb.append(String.format(
                "(%tF %<tR, %s, %s)",
                this.getDate(),
                this.getAuthor(),
                this.getRevisionString()));
        return sb.toString();
    }

    public GitWorkingCopy getWorkingCopy() {
        return this.wc;
    }

}
