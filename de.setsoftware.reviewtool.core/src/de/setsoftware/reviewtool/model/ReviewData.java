package de.setsoftware.reviewtool.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.CoreException;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * The data (collection of remarks and comments) belonging to a ticket. It is divided into multiple review
 * rounds, which contain the remarks.
 */
public class ReviewData {

    private final List<ReviewRound> rounds = new ArrayList<>();

    public ReviewData() {
    }

    ReviewData(List<ReviewRound> sortedRounds) {
        this.rounds.addAll(sortedRounds);
    }

    /**
     * Parse a review data object that has been serialized as a string.
     */
    public static ReviewData parse(
            ReviewStateManager p, IMarkerFactory m, String oldReviewData) {
        try {
            final BufferedReader r = new BufferedReader(new StringReader(oldReviewData));
            String line;
            final ReviewDataParser parser = new ReviewDataParser(p, m);
            while ((line = r.readLine()) != null) {
                final String trimmedLine = line.trim();
                parser.handleNextLine(trimmedLine);
            }
            parser.endLastItem();
            return parser.getResult();
        } catch (final IOException | CoreException e) {
            throw new ReviewtoolException(e);
        }
    }

    public void merge(ReviewRemark reviewRemark, int roundNumber) {
        this.getOrCreateRound(roundNumber).merge(reviewRemark);
    }

    private ReviewRound getOrCreateRound(int roundNumber) {
        final int roundIndex = Math.max(roundNumber - 1, 0);
        while (roundIndex >= this.rounds.size()) {
            this.rounds.add(new ReviewRound(this.rounds.size() + 1));
        }
        return this.rounds.get(roundIndex);
    }

    /**
     * Create a string representation of this review data.
     */
    public String serialize() {
        final ListIterator<ReviewRound> iter = this.rounds.listIterator(this.rounds.size());
        final StringBuilder ret = new StringBuilder();
        while (iter.hasPrevious()) {
            final ReviewRound round = iter.previous();
            if (!round.isEmpty()) {
                if (ret.length() > 0) {
                    ret.append("\n");
                }
                ret.append(round.serialize());
            }
        }
        return ret.toString();
    }

    /**
     * Returns true iff there are unresolved remarks in any of the review rounds.
     */
    public boolean hasUnresolvedRemarks() {
        for (final ReviewRound r : this.rounds) {
            if (r.hasUnresolvedRemarks()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true iff there are temporary markers in any of the review rounds.
     */
    public boolean hasTemporaryMarkers() {
        for (final ReviewRound r : this.rounds) {
            if (r.hasTemporaryMarkers()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes the given remark.
     */
    public void deleteRemark(ReviewRemark reviewRemark) {
        for (final ReviewRound r: this.rounds) {
            r.deleteRemark(reviewRemark);
        }
    }

}
