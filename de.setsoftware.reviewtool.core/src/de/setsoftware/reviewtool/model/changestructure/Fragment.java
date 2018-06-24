package de.setsoftware.reviewtool.model.changestructure;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import de.setsoftware.reviewtool.base.ReviewtoolException;
import de.setsoftware.reviewtool.model.api.IDelta;
import de.setsoftware.reviewtool.model.api.IFragment;
import de.setsoftware.reviewtool.model.api.IFragmentList;
import de.setsoftware.reviewtool.model.api.IPositionInText;
import de.setsoftware.reviewtool.model.api.IRevisionedFile;
import de.setsoftware.reviewtool.model.api.IncompatibleFragmentException;

/**
 * Default implementation of {@link IFragment}.
 */
public final class Fragment implements IFragment {

    private static final long serialVersionUID = 8223980588230543842L;

    private final IRevisionedFile file;
    private final IPositionInText from;
    private final IPositionInText to;
    private final Set<IFragment> origins;
    private String content;

    Fragment(final IRevisionedFile file, final IPositionInText from, final IPositionInText to,
            final IFragment... origins) {
        this(file, from, to, Arrays.asList(origins));
    }

    Fragment(final IRevisionedFile file, final IPositionInText from, final IPositionInText to,
            final Collection<? extends IFragment> origins) {
        this(file, from, to, combineOrigins(origins));
    }

    private Fragment(final IRevisionedFile file, final IPositionInText from, final IPositionInText to,
            final Set<? extends IFragment> origins) {
        assert file != null;
        assert from != null;
        assert to != null;
        this.file = file;
        this.from = from;
        this.to = to;

        // if the set of origins contains only one Fragment equal to this one, we omit it
        this.origins = new LinkedHashSet<>();
        if (origins.size() != 1 || !origins.iterator().next().equals(this)) {
            this.origins.addAll(origins);
        }
    }

    /**
     * Combines the origins into a single set.
     * @param origins A collection of fragments.
     * @return The resulting set.
     */
    private static Set<? extends IFragment> combineOrigins(final Collection<? extends IFragment> origins) {
        final Set<IFragment> newOrigins = new LinkedHashSet<>();
        for (final IFragment origin : origins) {
            newOrigins.addAll(origin.getOrigins());
        }
        return newOrigins;
    }

    /**
     * Factory method that creates a fragment with already set content string.
     * Mainly for unit tests. Normally, fragments should be created using
     * the {@link ChangestructureFactory}.
     */
    public static IFragment createWithContent(
            final IRevisionedFile file,
            final IPositionInText from,
            final IPositionInText to,
            final String content) {
        final Fragment ret = new Fragment(file, from, to);
        ret.content = content;
        return ret;
    }

    @Override
    public IRevisionedFile getFile() {
        return this.file;
    }

    @Override
    public IPositionInText getFrom() {
        return this.from;
    }

    @Override
    public IPositionInText getTo() {
        return this.to;
    }

    @Override
    public boolean isInline() {
        return this.from.getLine() == this.to.getLine();
    }

    @Override
    public IDelta getSize() {
        return this.to.minus(this.from);
    }

    @Override
    public Set<IFragment> getOrigins() {
        if (this.isOrigin()) {
            final Set<IFragment> result = new LinkedHashSet<>();
            result.add(this);
            return Collections.unmodifiableSet(result);
        } else {
            return Collections.unmodifiableSet(this.origins);
        }
    }

    @Override
    public String getContentFullLines() {
        if (this.content == null) {
            this.content = this.extractContent();
        }
        return this.content;
    }

    @Override
    public String getContent() {
        final String s = this.getContentFullLines();
        if (s.isEmpty()) {
            return s;
        }
        if (this.to.getColumn() > 1) {
            final int discardFromEnd = countCharsInLastLine(s) - this.to.getColumn() + 1;
            final int endIndex = s.length() - discardFromEnd;
            return s.substring(this.from.getColumn() - 1, endIndex);
        } else {
            return s.substring(this.from.getColumn() - 1);
        }
    }

    private static int countCharsInLastLine(final String s) {
        int count = s.endsWith("\n") ? 1 : 0;
        for (int i = s.length() - count - 1; i >= 0; i--) {
            if (s.charAt(i) == '\n') {
                break;
            } else {
                count++;
            }
        }
        return count;
    }

    private String extractContent() {
        if (this.isDeletion()) {
            return "";
        }

        final byte[] contents;
        try {
            contents = this.file.getContents();
        } catch (final Exception e) {
            return "?";
        }

        try {
            final BufferedReader r = new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(contents), "UTF-8"));
            final StringBuilder ret = new StringBuilder();
            int lineNumber = 1;
            String lineContent;
            while ((lineContent = r.readLine()) != null) {
                if (lineNumber >= this.from.getLine()) {
                    if (lineNumber < this.to.getLine()) {
                        ret.append(lineContent).append('\n');
                    } else if (lineNumber == this.to.getLine()) {
                        if (this.to.getColumn() > 1) {
                            ret.append(lineContent).append('\n');
                        }
                    }
                }
                lineNumber++;
            }
            return ret.toString();
        } catch (final IOException e) {
            throw new AssertionError("unexpected exception", e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(this.from.toString());
        result.append(" - ");
        result.append(this.to.toString());
        result.append(" in ");
        result.append(this.file.toString());
        if (this.origins.size() > 1) {
            result.append("\norigins: ");
            result.append(this.origins.toString().replace(", ", ",\n  "));
        } else if (this.origins.size() == 1) {
            result.append("\norigin: ");
            result.append(this.origins.iterator().next());
        }
        return result.toString();
    }

    @Override
    public boolean isOrigin() {
        return this.origins.isEmpty();
    }

    @Override
    public boolean isNeighboring(final IFragment other) {
        if (!this.file.equals(other.getFile())) {
            return false;
        }
        return this.isAdjacentTo(other);
    }

    @Override
    public boolean overlaps(final IFragment other) {
        return this.to.compareTo(other.getFrom()) > 0 && this.from.compareTo(other.getTo()) < 0;
    }

    @Override
    public boolean isAdjacentTo(final IFragment other) {
        return this.to.equals(other.getFrom()) || this.from.equals(other.getTo());
    }

    @Override
    public boolean containsChangeInOneOf(final Collection<? extends IFragment> fragments) {
        for (final IFragment origin : this.getOrigins()) {
            for (final IFragment fragment : fragments) {
                if (origin.overlaps(fragment) || origin.isAdjacentTo(fragment)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public IFragment adjoin(final IFragment other) {
        assert this.isAdjacentTo(other);
        if (this.to.equals(other.getFrom())) {
            return new Fragment(this.file, this.from, other.getTo(), this, other);
        } else {
            return new Fragment(this.file, other.getFrom(), this.to, this, other);
        }
    }

    @Override
    public IFragmentList subtract(final IFragment other) {
        if (!this.overlaps(other)) {
            return new FragmentList(this);
        } else {
            try {
                final IFragmentList fragmentList = new FragmentList();
                if (this.from.lessThan(other.getFrom())) {
                    fragmentList.addFragment(new Fragment(this.file, this.from, other.getFrom(), this));
                }
                if (other.getTo().lessThan(this.to)) {
                    fragmentList.addFragment(new Fragment(this.file, other.getTo(), this.to, this));
                }
                return fragmentList;
            } catch (final IncompatibleFragmentException e) {
                throw new ReviewtoolException(e);
            }
        }
    }

    @Override
    public IFragmentList subtract(final IFragmentList other) {
        return new FragmentList(this).subtract(other);
    }

    @Override
    public boolean canBeMergedWith(final IFragment other) {
        if (!this.file.equals(other.getFile())) {
            return false;
        }
        return this.isAdjacentTo(other) || this.overlaps(other);
    }

    @Override
    public IFragment merge(final IFragment other) {
        if (other.getFrom().lessThan(this.getFrom())) {
            return other.merge(this);
        }

        assert this.canBeMergedWith(other);
        final IPositionInText minFrom = this.getFrom();
        final IPositionInText maxTo;
        if (this.to.lessThan(other.getTo())) {
            maxTo = other.getTo();
        } else {
            maxTo = this.to;
        }
        return new Fragment(this.file, minFrom, maxTo, this, other);
    }

    @Override
    public boolean isDeletion() {
        return this.to.equals(this.from);
    }

    @Override
    public IFragment setFile(final IRevisionedFile newFile) {
        return new Fragment(newFile, this.from, this.to, this);
    }

    @Override
    public int hashCode() {
        return this.from.hashCode() + 31 * this.file.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Fragment)) {
            return false;
        }
        final Fragment other = (Fragment) obj;
        return this.file.equals(other.file)
            && this.from.equals(other.from)
            && this.to.equals(other.to)
            && this.origins.equals(other.origins);
    }

    @Override
    public int getNumberOfLines() {
        return this.to.getLine() - this.from.getLine();
    }

    @Override
    public IFragment adjust(final IDelta delta) {
        return new Fragment(
                this.file,
                this.from.plus(delta),
                this.to.plus(this.isInline() ? delta : delta.ignoreColumnOffset()),
                this);
    }

    @Override
    public int compareTo(final IFragment o) {
        final int from = this.getFrom().compareTo(o.getFrom());
        return from != 0 ? from : this.getTo().compareTo(o.getTo());
    }

}
