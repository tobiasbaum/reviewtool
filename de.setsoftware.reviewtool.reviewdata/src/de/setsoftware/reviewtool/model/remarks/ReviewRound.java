package de.setsoftware.reviewtool.model.remarks;

import java.util.ArrayList;
import java.util.Arrays;
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

    public static final String[] POSITIVE_HEADER = new String[] {"positiv"};
    public static final String[] MUST_FIX_HEADER = new String[] {"muss", "wichtig"};
    public static final String[] CAN_FIX_HEADER = new String[] {"kann", "optional / weniger wichtig"};
    public static final String[] ALREADY_FIXED_HEADER = new String[] {"direkt eingepflegt"};
    public static final String[] TEMPORARY_HEADER = new String[] {"temporärer Marker"};
    public static final String[] OTHER_REMARK_HEADER = new String[] {"sonstige Anmerkungen"};

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
            this.add(reviewRemark);
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
        final int index = this.findInsertionIndex(reviewRemark);
        this.remarks.add(index, reviewRemark);
    }

    private int findInsertionIndex(ReviewRemark reviewRemark) {
        for (int i = 0; i < this.remarks.size(); i++) {
            if (this.remarks.get(i).hasLargerPositionThan(reviewRemark)) {
                return i;
            }
        }
        return this.remarks.size();
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
        this.serializeRemarksWithType(OTHER_REMARK_HEADER[0], ret, RemarkType.OTHER);
        this.serializeRemarksWithType(POSITIVE_HEADER[0], ret, RemarkType.POSITIVE);
        this.serializeRemarksWithType(MUST_FIX_HEADER[0], ret, RemarkType.MUST_FIX);
        this.serializeRemarksWithType(CAN_FIX_HEADER[0], ret, RemarkType.CAN_FIX);
        this.serializeRemarksWithType(ALREADY_FIXED_HEADER[0], ret, RemarkType.ALREADY_FIXED);
        this.serializeRemarksWithType(TEMPORARY_HEADER[0], ret, RemarkType.TEMPORARY);
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
        if (Arrays.asList(ALREADY_FIXED_HEADER).contains(string)) {
            return RemarkType.ALREADY_FIXED;
        } else if (Arrays.asList(CAN_FIX_HEADER).contains(string)) {
            return RemarkType.CAN_FIX;
        } else if (Arrays.asList(MUST_FIX_HEADER).contains(string)) {
            return RemarkType.MUST_FIX;
        } else if (Arrays.asList(POSITIVE_HEADER).contains(string)) {
            return RemarkType.POSITIVE;
        } else if (Arrays.asList(TEMPORARY_HEADER).contains(string)) {
            return RemarkType.TEMPORARY;
        } else if (Arrays.asList(OTHER_REMARK_HEADER).contains(string)) {
            return RemarkType.OTHER;
        } else {
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

    /**
     * Returns true iff this review round contains a remark with the same message and position as the given one.
     */
    public boolean contains(ReviewRemark reviewRemark) {
        return this.findSimilar(reviewRemark) >= 0;
    }

    /**
     * Returns the remarks.
     */
    public List<ReviewRemark> getRemarks() {
        return this.remarks;
    }

}
