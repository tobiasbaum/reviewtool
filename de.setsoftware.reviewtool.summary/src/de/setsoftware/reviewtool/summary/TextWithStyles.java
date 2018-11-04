package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * A string with formatting information.
 * Is mutable (i.e. more like a StringBuilder than a String).
 */
public class TextWithStyles {

    private static final AtomicReference<Color> GRAY = new AtomicReference<>();

    private String text = "";
    private final List<StyleRange> styles = new ArrayList<>();

    /**
     * Adds text without formatting.
     */
    public TextWithStyles addNormal(String s) {
        this.text += s;
        return this;
    }

    /**
     * Adds italic text.
     */
    public TextWithStyles addItalic(String s) {
        final StyleRange r = new StyleRange(this.text.length(), s.length(), null, null, SWT.ITALIC);
        this.text += s;
        this.styles.add(r);
        return this;
    }

    /**
     * Adds text in dark gray color.
     */
    public TextWithStyles addGray(String s) {
        if (GRAY.get() == null) {
            final Display display = PlatformUI.getWorkbench().getDisplay();
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    GRAY.set(display.getSystemColor(SWT.COLOR_DARK_GRAY));
                }
            });
        }
        final StyleRange r = new StyleRange(this.text.length(), s.length(), GRAY.get(), null);
        this.text += s;
        this.styles.add(r);
        return this;
    }

    /**
     * Adds pre-styled text.
     */
    public TextWithStyles add(TextWithStyles textWithStyles) {
        this.styles.addAll(textWithStyles.getStyles(this.text.length()));
        this.text += textWithStyles.text;
        return this;
    }

    /**
     * Returns the raw text.
     */
    public String getText() {
        return this.text;
    }

    /**
     * Returns the styles, with start offsets adjusted for the provided offset.
     */
    public List<? extends StyleRange> getStyles(int startOffset) {
        final List<StyleRange> ret = new ArrayList<>();
        for (final StyleRange r : this.styles) {
            final StyleRange moved = (StyleRange) r.clone();
            moved.start += startOffset;
            ret.add(moved);
        }
        return ret;
    }

    /**
     * Splits the text along line-breaks and returns the results.
     */
    public List<TextWithStyles> splitLines() {
        final List<TextWithStyles> lines = new ArrayList<>();
        int last = 0;
        for (int i = 0; i < this.text.length(); i++) {
            if (this.text.charAt(i) == '\n') {
                lines.add(this.substring(last, i));
                last = i + 1;
            }
        }
        if (last < this.text.length()) {
            lines.add(this.substring(last, this.text.length()));
        }
        return lines;
    }

    private TextWithStyles substring(int beginIndex, int endIndex) {
        final TextWithStyles ret = new TextWithStyles();
        ret.text = this.text.substring(beginIndex, endIndex);
        for (final StyleRange r : this.styles) {
            if (overlaps(r, beginIndex, endIndex)) {
                ret.styles.add(extract(r, beginIndex, endIndex));
            }
        }
        return ret;
    }

    private static StyleRange extract(StyleRange r, int beginIndex, int endIndex) {
        final int start = Math.max(0, r.start - beginIndex);
        final int end = Math.min(endIndex, r.start + r.length) - beginIndex;
        final int length = end - start;
        if (r.start == start && r.length == length) {
            return r;
        } else {
            final StyleRange ret = (StyleRange) r.clone();
            ret.start = start;
            ret.length = length;
            return ret;
        }
    }

    private static boolean overlaps(StyleRange r, int beginIndex, int endIndex) {
        return r.start < endIndex && r.start + r.length > beginIndex;
    }

    public static TextWithStyles italic(String string) {
        return new TextWithStyles().addItalic(string);
    }

}
