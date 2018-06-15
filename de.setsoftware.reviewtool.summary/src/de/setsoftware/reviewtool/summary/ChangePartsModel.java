package de.setsoftware.reviewtool.summary;

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

    /**
     * Sort parts using relevance or alphabetic, if relevance is same.
     */
    public void sort() {
        this.newParts.sort();
        this.deletedParts.sort();
        this.changedParts.sort();
    }
}
