package de.setsoftware.reviewtool.model.remarks;

import java.util.ArrayList;
import java.util.List;

/**
 * A review round corresponds to the phase between implementation and fixing.
 * When the author ends fixing and a reviewer starts to review again, this is a new
 * review round.
 *
 * <p>A review round stores all remarks that were first made during this round. Comments
 * on issues raised earlier are stored at these earlier rounds.
 */
public class ReviewRound {

    public static final String POSITIVE_HEADER = "positiv";
    public static final String MUST_FIX_HEADER = "muss";
    public static final String CAN_FIX_HEADER = "kann";
    public static final String ALREADY_FIXED_HEADER = "direkt eingepflegt";
    public static final String TEMPORARY_HEADER = "temporärer Marker";
    public static final String OTHER_REMARK_HEADER = "sonstige Anmerkungen";

    private final int nbr;
    private final List<ReviewRemark> remarks = new ArrayList<>();

    public ReviewRound(int nbr) {
        this.nbr = nbr;
    }

    public int getNumber() {
        return this.nbr;
    }

    /**
     * Merges the given review remarks into the remarks for this round: If a similar remark
     * is existing, it is replaced, otherwise it is added as new.
     */
    public void merge(ReviewRemark reviewRemark) throws ReviewRemarkException {
        final int index = this.findSimilar(reviewRemark);
        if (index >= 0) {
            this.remarks.set(index, reviewRemark);
        } else {
            this.remarks.add(reviewRemark);
        }
    }

    private int findSimilar(ReviewRemark reviewRemark) throws ReviewRemarkException {
        int i = 0;
        for (final ReviewRemark r : this.remarks) {
            if (r.hasSameTextAndPositionAs(reviewRemark)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void add(ReviewRemark reviewRemark) {
        this.remarks.add(reviewRemark);
    }

    public boolean isEmpty() {
        return this.remarks.isEmpty();
    }

    /**
     * Creates a String representation of this review round.
     */
    public String serialize() throws ReviewRemarkException {
        final StringBuilder ret = new StringBuilder();
        ret.append("Review ").append(this.nbr).append(":\n");
        this.serializeRemarksWithType(OTHER_REMARK_HEADER, ret, RemarkType.OTHER);
        this.serializeRemarksWithType(POSITIVE_HEADER, ret, RemarkType.POSITIVE);
        this.serializeRemarksWithType(MUST_FIX_HEADER, ret, RemarkType.MUST_FIX);
        this.serializeRemarksWithType(CAN_FIX_HEADER, ret, RemarkType.CAN_FIX);
        this.serializeRemarksWithType(ALREADY_FIXED_HEADER, ret, RemarkType.ALREADY_FIXED);
        this.serializeRemarksWithType(TEMPORARY_HEADER, ret, RemarkType.TEMPORARY);
        return ret.toString();
    }

    private void serializeRemarksWithType(String title, StringBuilder ret, RemarkType type)
            throws ReviewRemarkException {
        boolean titleWritten = false;
        for (final ReviewRemark remark : this.remarks) {
            if (remark.getRemarkType() == type) {
                if (!titleWritten) {
                    ret.append("* ").append(title).append("\n");
                    titleWritten = true;
                }
                ret.append(remark.serialize());
            }
        }
    }

    /**
     * Parses the string representation of the remark type.
     */
    public static RemarkType parseType(String string) {
        switch (string) {
        case ALREADY_FIXED_HEADER:
            return RemarkType.ALREADY_FIXED;
        case CAN_FIX_HEADER:
            return RemarkType.CAN_FIX;
        case MUST_FIX_HEADER:
            return RemarkType.MUST_FIX;
        case POSITIVE_HEADER:
            return RemarkType.POSITIVE;
        case TEMPORARY_HEADER:
            return RemarkType.TEMPORARY;
        case OTHER_REMARK_HEADER:
            return RemarkType.OTHER;
        default:
            throw new ReviewRemarkException("parse exception: " + string);
        }
    }

    /**
     * Returns true iff there exist remarks in this review round that still
     * need fixing.
     */
    public boolean hasUnresolvedRemarks() throws ReviewRemarkException {
        for (final ReviewRemark r : this.remarks) {
            if (r.needsFixing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return true iff there are temporary markers in this review round.
     */
    public boolean hasTemporaryMarkers() throws ReviewRemarkException {
        for (final ReviewRemark r : this.remarks) {
            if (r.getRemarkType() == RemarkType.TEMPORARY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes the given remark from this review round.
     * If it is not contained, nothing happens.
     */
    public void deleteRemark(ReviewRemark reviewRemark) throws ReviewRemarkException {
        final int i = this.findSimilar(reviewRemark);
        if (i >= 0) {
            this.remarks.remove(i);
        }
    }

}
