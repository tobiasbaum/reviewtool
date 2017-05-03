package de.setsoftware.reviewtool.diffalgorithms;

/**
 * Helper class for shifting the diff. Encapsulates the suitability of a certain line to be the start line
 * of a hunk.
 */
class StartLineSuitability implements Comparable<StartLineSuitability> {

    /**
     * Different types of lines taken into account.
     * The order of this enum determines the suitability. The further down, the better.
     */
    private static enum Type {
        LINE_WITH_END_MARKER,
        EMPTY_LINE,
        OTHER_LINE,
        LINE_WITH_START_MARKER,
    }

    private final Type type;
    private final int indentation;
    private final int markerGoodness;

    private StartLineSuitability(Type type, int countIndent, int markerGoodness) {
        this.type = type;
        this.indentation = countIndent;
        this.markerGoodness = markerGoodness;
    }

    public static StartLineSuitability determineFor(String line) {
        final String lineTrim = line.trim();
        final Type type;
        final int markerGoodness;
        if (lineTrim.startsWith("/*")) {
            type = Type.LINE_WITH_START_MARKER;
            markerGoodness = 2;
        } else if (lineTrim.endsWith("{")) {
            type = Type.LINE_WITH_START_MARKER;
            markerGoodness = 1;
        } else if (lineTrim.startsWith("</")) {
            type = Type.LINE_WITH_END_MARKER;
            markerGoodness = 1;
        } else if (lineTrim.isEmpty()) {
            type = Type.EMPTY_LINE;
            markerGoodness = 0;
        } else {
            type = Type.OTHER_LINE;
            markerGoodness = 0;
        }
        return new StartLineSuitability(type, countIndent(line), markerGoodness);
    }

    private static int countIndent(String line) {
        int cnt = 0;
        for (final char ch : line.toCharArray()) {
            if (ch == ' ') {
                cnt++;
            } else if (ch == '\t') {
                cnt += 4;
            } else {
                break;
            }
        }
        return cnt;
    }

    @Override
    public int compareTo(StartLineSuitability o) {
        int cmp;
        cmp = Integer.compare(this.type.ordinal(), o.type.ordinal());
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(o.indentation, this.indentation);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(this.markerGoodness, o.markerGoodness);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StartLineSuitability)) {
            return false;
        }
        final StartLineSuitability s = (StartLineSuitability) o;
        return this.compareTo(s) == 0;
    }

    @Override
    public int hashCode() {
        return this.type.hashCode() + this.indentation + this.markerGoodness;
    }

}
