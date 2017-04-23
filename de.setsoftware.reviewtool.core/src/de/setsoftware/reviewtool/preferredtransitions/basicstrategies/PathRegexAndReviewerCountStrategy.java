package de.setsoftware.reviewtool.preferredtransitions.basicstrategies;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import de.setsoftware.reviewtool.model.ITicketData;
import de.setsoftware.reviewtool.model.changestructure.Stop;
import de.setsoftware.reviewtool.model.changestructure.Tour;
import de.setsoftware.reviewtool.model.changestructure.ToursInReview;
import de.setsoftware.reviewtool.preferredtransitions.api.IPreferredTransitionStrategy;

/**
 * Returns a certain transition as preferred when a path under review matches a regular expression.
 */
public class PathRegexAndReviewerCountStrategy implements IPreferredTransitionStrategy {

    private final Pattern pattern;
    private final String transitionName;
    private final int maxReviewerCount;

    public PathRegexAndReviewerCountStrategy(
            String regex, String transitionName, int maxReviewerCount) {
        this.pattern = Pattern.compile(regex);
        this.transitionName = transitionName;
        this.maxReviewerCount = maxReviewerCount;
    }

    @Override
    public List<String> determinePreferredTransitions(
            boolean forOkCase, ITicketData ticketData, ToursInReview toursInReview) {
        if (!forOkCase) {
            return Collections.emptyList();
        }
        if (ticketData.getTicketInfo().getReviewers().size() > this.maxReviewerCount) {
            return Collections.emptyList();
        }

        for (final Tour t : toursInReview.getTours()) {
            for (final Stop s : t.getStops()) {
                if (this.pattern.matcher(s.getMostRecentFile().getPath()).matches()) {
                    return Collections.singletonList(this.transitionName);
                }
            }
        }
        return Collections.emptyList();
    }

}
