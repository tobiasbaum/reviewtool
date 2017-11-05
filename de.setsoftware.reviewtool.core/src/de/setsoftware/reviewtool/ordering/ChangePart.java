package de.setsoftware.reviewtool.ordering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.changestructure.Stop;

/**
 * A collection of stops with the minimum granularity for sorting/grouping.
 * Currently, the minimum granularity is at the level of methods, i.e. all stops/hunks in the same method
 * will be regarded together.
 */
public class ChangePart {

    private final List<Stop> stops;

    public ChangePart(List<Stop> stops) {
        this.stops = stops;
    }

    /**
     * Returns the stops belonging to this change part. Every change part consists of at least one stop.
     */
    public List<Stop> getStops() {
        return this.stops;
    }

    @Override
    public int hashCode() {
        return this.stops.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChangePart)) {
            return false;
        }
        return ((ChangePart) o).stops.equals(this.stops);
    }

    @Override
    public String toString() {
        return this.stops.toString();
    }

    /**
     * Returns true iff all contained stops are irrelevant for review.
     */
    public boolean isFullyIrrelevantForReview() {
        for (final Stop s : this.stops) {
            if (!s.isIrrelevantForReview()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Groups the given stops into change parts.
     */
    public static List<ChangePart> groupToMinimumGranularity(List<Stop> stopsToGroup) {
        //sort by file and by line inside the files
        final List<Stop> sortedStops = new ArrayList<>(stopsToGroup);
        Collections.sort(sortedStops, new Comparator<Stop>() {
            @Override
            public int compare(Stop o1, Stop o2) {
                final int cmp = o1.getOriginalMostRecentFile().getPath().compareTo(
                        o2.getOriginalMostRecentFile().getPath());
                if (cmp != 0) {
                    return cmp;
                }
                return Integer.compare(this.getLine(o1), this.getLine(o2));
            }

            private int getLine(Stop o2) {
                final IFragment fragment = o2.getOriginalMostRecentFragment();
                return fragment == null ? -1 : fragment.getFrom().getLine();
            }
        });

        final List<Stop> stopsInCurrentFile = new ArrayList<>();
        IRevisionedFile currentFile = null;
        final List<ChangePart> ret = new ArrayList<>();
        for (final Stop s : sortedStops) {
            if (currentFile == null || !currentFile.equals(s.getOriginalMostRecentFile())) {
                groupStopsInFile(ret, stopsInCurrentFile);
                currentFile = s.getOriginalMostRecentFile();
                stopsInCurrentFile.clear();
            }
            stopsInCurrentFile.add(s);
        }
        groupStopsInFile(ret, stopsInCurrentFile);
        return ret;
    }

    private static void groupStopsInFile(List<ChangePart> resultBuffer, List<Stop> stopsInCurrentFile) {

        if (stopsInCurrentFile.isEmpty()) {
            return;
        }
        if (stopsInCurrentFile.size() == 1) {
            //optimization: don't parse the file if it is not needed
            resultBuffer.add(new ChangePart(Collections.singletonList(stopsInCurrentFile.get(0))));
            return;
        }
        if (stopsInCurrentFile.get(0).isBinaryChange()) {
            for (final Stop s : stopsInCurrentFile) {
                resultBuffer.add(new ChangePart(Collections.singletonList(s)));
            }
            return;
        }

        //TODO check if java file

        final byte[] contents;
        try {
            contents = stopsInCurrentFile.get(0).getOriginalMostRecentFile().getContents();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (final Exception e) {
            throw new ReviewtoolException(e);
        }
        if (contents == null) {
            for (final Stop s : stopsInCurrentFile) {
                resultBuffer.add(new ChangePart(Collections.singletonList(s)));
            }
            return;
        }

        final IslandScanner scanner = new IslandScanner(contents);
        int currentBlockEnd = scanner.getNextRegionEndLineNumber();
        List<Stop> stopsInCurrentBlock = new ArrayList<>();
        for (final Stop s : stopsInCurrentFile) {
            final int stopStartLine = s.getOriginalMostRecentFragment().getFrom().getLine();
            if (stopStartLine <= currentBlockEnd) {
                //stop is still in current block
                stopsInCurrentBlock.add(s);
            } else {
                //stop belongs to one of the next block, end the current one and determine new block
                if (!stopsInCurrentBlock.isEmpty()) {
                    resultBuffer.add(new ChangePart(stopsInCurrentBlock));
                }
                do {
                    currentBlockEnd = scanner.getNextRegionEndLineNumber();
                } while (stopStartLine > currentBlockEnd);
                stopsInCurrentBlock = new ArrayList<>();
                stopsInCurrentBlock.add(s);
            }
        }
        resultBuffer.add(new ChangePart(stopsInCurrentBlock));
    }

    /**
     * The token types that the {@link IslandScanner} can emit.
     */
    private static enum TokenType {
        MULTILINE_COMMENT_START,
        MULTILINE_COMMENT_END,
        SINGLE_LINE_COMMENT,
        BRACE_OPEN,
        BRACE_CLOSE,
        SEMI,
        SEA,
    }

    /**
     * State for the {@link IslandScanner}'s automaton.
     */
    private static enum IslandScannerState {
        INIT,
        GENERIC_CONTENT,
        POSSIBLE_COMMENT_START,
        IN_SINGLELINE_COMMENT,
        IN_MULTILINE_COMMENT,
        POSSIBLE_COMMENT_END,
        CHAR,
        CHAR_ESCAPE,
        STRING,
        STRING_ESCAPE
    }

    /**
     * Lexer for part of the java grammar (cmp island grammars).
     */
    private static final class IslandScanner {

        private final byte[] content;
        private int pos;
        private IslandScannerState previousEndState;

        private int nestingDepth;
        private int currentLineNumber;

        public IslandScanner(byte[] content) {
            this.content = content;
            this.pos = 0;
            this.previousEndState = IslandScannerState.INIT;
            this.currentLineNumber = 0;
        }

        public int getNextRegionEndLineNumber() {
            while (this.pos < this.content.length) {
                this.currentLineNumber++;
                boolean hadPotentialEnd = false;
                final List<TokenType> tokens = this.parseNextLine();
                for (final TokenType token : tokens) {
                    switch (token) {
                    case BRACE_OPEN:
                        hadPotentialEnd = true;
                        this.nestingDepth++;
                        break;
                    case BRACE_CLOSE:
                        hadPotentialEnd = true;
                        this.nestingDepth--;
                        break;
                    case SEMI:
                        hadPotentialEnd = true;
                        break;
                    default:
                        //other token types not needed
                        break;
                    }
                }
                if (this.nestingDepth >= 2) {
                    continue;
                }
                if (hadPotentialEnd || tokens.isEmpty()) {
                    return this.currentLineNumber;
                }
            }
            return Integer.MAX_VALUE;
        }

        public List<TokenType> parseNextLine() {
            final List<TokenType> ret = new ArrayList<>();
            IslandScannerState state;
            if (this.previousEndState == IslandScannerState.IN_MULTILINE_COMMENT) {
                state = IslandScannerState.IN_MULTILINE_COMMENT;
            } else {
                state = IslandScannerState.INIT;
            }
            while (this.pos < this.content.length) {
                final byte cur = this.content[this.pos++];
                if (cur == '\n') {
                    break;
                }
                switch (state) {
                case INIT:
                    switch (cur) {
                    case '/':
                        state = IslandScannerState.POSSIBLE_COMMENT_START;
                        break;
                    case '"':
                        state = IslandScannerState.STRING;
                        break;
                    case '\'':
                        state = IslandScannerState.CHAR;
                        break;
                    case ' ':
                    case '\t':
                    case '\r':
                        break;
                    case '{':
                        ret.add(TokenType.BRACE_OPEN);
                        break;
                    case '}':
                        ret.add(TokenType.BRACE_CLOSE);
                        break;
                    case ';':
                        ret.add(TokenType.SEMI);
                        break;
                    default:
                        state = IslandScannerState.GENERIC_CONTENT;
                        break;
                    }
                    break;
                case GENERIC_CONTENT:
                    switch (cur) {
                    case '/':
                        ret.add(TokenType.SEA);
                        state = IslandScannerState.POSSIBLE_COMMENT_START;
                        break;
                    case '"':
                        state = IslandScannerState.STRING;
                        break;
                    case '\'':
                        state = IslandScannerState.CHAR;
                        break;
                    case '{':
                        ret.add(TokenType.BRACE_OPEN);
                        break;
                    case '}':
                        ret.add(TokenType.BRACE_CLOSE);
                        break;
                    case ';':
                        ret.add(TokenType.SEMI);
                        break;
                    default:
                        break;
                    }
                    break;
                case POSSIBLE_COMMENT_START:
                    switch (cur) {
                    case '*':
                        ret.add(TokenType.MULTILINE_COMMENT_START);
                        state = IslandScannerState.IN_MULTILINE_COMMENT;
                        break;
                    case '/':
                        ret.add(TokenType.SINGLE_LINE_COMMENT);
                        state = IslandScannerState.IN_SINGLELINE_COMMENT;
                        break;
                    default:
                        state = IslandScannerState.GENERIC_CONTENT;
                        break;
                    }
                    break;
                case IN_SINGLELINE_COMMENT:
                    break;
                case IN_MULTILINE_COMMENT:
                    switch (cur) {
                    case '*':
                        state = IslandScannerState.POSSIBLE_COMMENT_END;
                        break;
                    default:
                        break;
                    }
                    break;
                case POSSIBLE_COMMENT_END:
                    switch (cur) {
                    case '/':
                        ret.add(TokenType.MULTILINE_COMMENT_END);
                        state = IslandScannerState.GENERIC_CONTENT;
                        break;
                    case '*':
                        break;
                    default:
                        state = IslandScannerState.IN_MULTILINE_COMMENT;
                        break;
                    }
                    break;
                case STRING:
                    switch (cur) {
                    case '"':
                        state = IslandScannerState.GENERIC_CONTENT;
                        break;
                    case '\\':
                        state = IslandScannerState.STRING_ESCAPE;
                        break;
                    default:
                        break;
                    }
                    break;
                case CHAR:
                    switch (cur) {
                    case '\'':
                        state = IslandScannerState.GENERIC_CONTENT;
                        break;
                    case '\\':
                        state = IslandScannerState.CHAR_ESCAPE;
                        break;
                    default:
                        break;
                    }
                    break;
                case STRING_ESCAPE:
                    state = IslandScannerState.STRING;
                    break;
                case CHAR_ESCAPE:
                    state = IslandScannerState.CHAR;
                    break;
                default:
                    throw new AssertionError("unsupported state " + state);
                }
            }
            if (state != IslandScannerState.INIT && ret.isEmpty()) {
                ret.add(TokenType.SEA);
            }
            this.previousEndState = state;
            return ret;
        }

    }

}
