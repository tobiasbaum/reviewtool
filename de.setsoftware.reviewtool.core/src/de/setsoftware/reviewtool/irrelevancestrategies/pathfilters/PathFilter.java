package de.setsoftware.reviewtool.irrelevancestrategies.pathfilters;

import java.util.regex.Pattern;

import de.setsoftware.reviewtool.model.changestructure.Change;
import de.setsoftware.reviewtool.model.changestructure.IIrrelevanceDetermination;

/**
 * Marks changes as irrelevant when both the old and the new path matches a pattern.
 */
public class PathFilter implements IIrrelevanceDetermination {

    private final String description;
    private final Pattern pattern;

    public PathFilter(String pattern, String description) {
        this.pattern = convertAntToRegex(pattern);
        this.description = description;
    }

    private static Pattern convertAntToRegex(String antStylePattern) {
        final StringBuilder part = new StringBuilder();
        final StringBuilder regex = new StringBuilder();
        for (final char ch : antStylePattern.toCharArray()) {
            if (ch == '/' || ch == '\\') {
                if (part.toString().equals("*")) {
                    regex.append("[^\\\\/]*");
                    part.setLength(0);
                } else {
                    regex.append(Pattern.quote(part.toString()));
                    part.setLength(0);
                }
                regex.append("(^|[\\\\/])");
            } else if (ch == '*') {
                if (part.toString().equals("*")) {
                    regex.append(".*");
                    part.setLength(0);
                } else {
                    regex.append(Pattern.quote(part.toString()));
                    part.setLength(0);
                    part.append(ch);
                }
            } else {
                if (part.toString().equals("*")) {
                    regex.append("[^\\\\/]*");
                    part.setLength(0);
                }
                part.append(ch);
            }
        }
        if (part.toString().equals("*")) {
            regex.append(".*");
            part.setLength(0);
        } else {
            regex.append(Pattern.quote(part.toString()));
            part.setLength(0);
        }
        return Pattern.compile(regex.toString());
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean isIrrelevant(Change change) {
        return this.pattern.matcher(change.getFrom().getPath()).matches()
            && this.pattern.matcher(change.getTo().getPath()).matches();
    }

}
