package de.setsoftware.reviewtool.viewtracking;

import java.util.HashMap;
import java.util.Map;

/**
 * Statistics on if and how long portions of a single file have been viewed.
 */
public class ViewStatisticsForFile {

    private int unspecificCount;
    private final Map<Integer, Integer> countsPerLine = new HashMap<>();

    /**
     * Marks that the given portion of the file has been viewed for one time slot.
     */
    public void mark(int fromLine, int toLine) {
        if (toLine < fromLine) {
            this.increaseCount(fromLine);
            return;
        }
        for (int line = fromLine; line <= toLine; line++) {
            this.increaseCount(line);
        }
    }

    private void increaseCount(int line) {
        final Integer oldCount = this.countsPerLine.get(line);
        if (oldCount == null) {
            this.countsPerLine.put(line, 1);
        } else if (oldCount < Integer.MAX_VALUE) {
            this.countsPerLine.put(line, oldCount + 1);
        }
    }

    /**
     * Marks that the file has been viewed for one time slot when no specific information
     * on the viewed part of the file is available.
     */
    public void markUnknownPosition() {
        if (this.unspecificCount < Integer.MAX_VALUE) {
            this.unspecificCount++;
        }
    }

    public ViewStatDataForStop determineViewRatioWithoutPosition(int longEnoughCount) {
        final double ratio = this.determineRatio(this.unspecificCount, longEnoughCount);
        return new ViewStatDataForStop(ratio, ratio);
    }

    private double determineRatio(int actualCount, int longEnoughCount) {
        final int boundedActual = Math.min(actualCount, longEnoughCount);
        return ((double) boundedActual) / longEnoughCount;
    }

    /**
     * Determines the view ratio for the given part of the file.
     * Both line numbers are inclusive. If they denote an empty span (probably deletion),
     * lineFrom is used.
     */
    public ViewStatDataForStop determineViewRatio(int lineFrom, int lineTo, int longEnoughCount) {
        if (this.countsPerLine.isEmpty()) {
            //no marks on line basis => fallback to whole file
            return this.determineViewRatioWithoutPosition(longEnoughCount);
        }
        if (lineTo < lineFrom) {
            return this.determineViewRatio(lineFrom, lineFrom, longEnoughCount);
        }
        double sum = 0.0;
        int count = 0;
        double max = 0.0;
        for (int line = lineFrom; line <= lineTo; line++) {
            final Integer countForLine = this.countsPerLine.get(line);
            final double lineRatio = this.determineRatio(countForLine == null ? 0 : countForLine, longEnoughCount);
            sum += lineRatio;
            count++;
            max = Math.max(max, lineRatio);
        }
        return new ViewStatDataForStop(sum / count, max);
    }

}
