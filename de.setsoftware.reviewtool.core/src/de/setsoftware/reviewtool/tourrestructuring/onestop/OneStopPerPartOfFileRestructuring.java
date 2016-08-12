package de.setsoftware.reviewtool.tourrestructuring.onestop;

import java.util.ArrayList;
import java.util.List;

import de.setsoftware.reviewtool.model.changestructure.ITourRestructuring;
import de.setsoftware.reviewtool.model.changestructure.Tour;

/**
 * Tour restructuring that merges all stops that can be merged (i.e. changes that
 * occurred in the same part of the file) into one stop. Tours that are emptied this
 * way are removed.
 */
public class OneStopPerPartOfFileRestructuring implements ITourRestructuring {

    @Override
    public String getDescription() {
        return "merge all changes in the same part of a file into one stop";
    }

    @Override
    public List<? extends Tour> restructure(List<Tour> originalTours) {
        if (originalTours.size() <= 1) {
            return null;
        }

        final List<MutableTour> mutableTours = new ArrayList<>();
        for (final Tour t : originalTours) {
            mutableTours.add(new MutableTour(t));
        }

        //to avoid situations where some changes are merged into a small fix commit,
        //  first resolve all tours that are fully resolvable and only merge the
        //  rest afterwards
        boolean didSomething;
        didSomething = this.resolveToursIfFullyResolvable(mutableTours);
        didSomething |= this.mergeAllMergeableStops(mutableTours);

        if (didSomething) {
            return MutableTour.toTours(mutableTours);
        } else {
            return null;
        }
    }

    private boolean resolveToursIfFullyResolvable(List<MutableTour> mutableTours) {
        boolean didSomething = false;
        for (int i = 0; i < mutableTours.size(); i++) {
            final MutableTour curTour = mutableTours.get(i);
            if (curTour.canBeResolvedCompletely(mutableTours, i)) {
                curTour.resolve(mutableTours, i);
                assert mutableTours.get(i).isEmpty();
                mutableTours.remove(i);
                i--;
                didSomething = true;
            }
        }
        return didSomething;
    }

    private boolean mergeAllMergeableStops(List<MutableTour> mutableTours) {
        boolean didSomething = false;
        for (int i = 0; i < mutableTours.size(); i++) {
            final MutableTour curTour = mutableTours.get(i);
            didSomething |= curTour.resolve(mutableTours, i);
            if (curTour.isEmpty()) {
                mutableTours.remove(i);
                i--;
            }
        }
        return didSomething;
    }

}
