package de.setsoftware.reviewtool.summary;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * Single part of a summary text that can be folded.
 */
class SummaryTextPart implements IRegion {
    private TextWithStyles text = new TextWithStyles().addNormal("");
    private TextWithStyles textFolded = new TextWithStyles().addNormal("");
    private int lines = 0;
    private int linesFolded = 0;
    private boolean folded = false;
    private boolean hasLink = false; // No hyper-link, if content is short enough

    private int linkOffset = 0; // Character offset of whole summary text, used for displaying links
    private int linkLength = 0; // Links with length 0 are not presented
    private int offsetAtStartOfPart;

    public SummaryTextPart() {
    }

    public SummaryTextPart(TextWithStyles refDiff) {
        this.text = refDiff;
        this.lines = refDiff.splitLines().size();
    }

    @Override
    public int getLength() {
        return this.linkLength;
    }

    @Override
    public int getOffset() {
        return this.linkOffset;
    }

    public void addLine(String string) {
        this.text.addNormal(string + "\n");
        this.lines++;
    }

    public void addLine(TextWithStyles text) {
        this.text.add(text).addNormal("\n");
        this.lines++;
    }

    public void addLinkIfNeeded(int maxLinesFolded) {
        if (this.lines > maxLinesFolded) {
            final List<TextWithStyles> splitText = this.text.splitLines();
            final int max = Math.max(2, maxLinesFolded);
            this.textFolded = new TextWithStyles();
            for (int i = 0; i < max - 1; i++) {
                this.textFolded.add(splitText.get(i)).addNormal("\n");
                this.linesFolded++;
            }
            this.hasLink = true;
            this.folded = true;
            this.linkLength = 7; // Link length (more.. or less...)
            this.text.addNormal("less..." + "\n\n");
            this.textFolded.addNormal("(" + (this.lines - this.linesFolded) + " more lines) show...\n\n");
        } else {
            this.text.addNormal("\n");
        }
    }

    public int getLineCount() {
        return this.lines;
    }

    public int getVisibleTextLength() {
        return this.getVisibleText().length();
    }

    public String getVisibleText() {
        if (this.folded) {
            return this.textFolded.getText();
        } else {
            return this.text.getText();
        }
    }

    public int getVisibleLineCount() {
        if (this.folded) {
            return this.linesFolded;
        } else {
            return this.lines;
        }
    }

    public void updateOffsets(int offsetAtEndOfPart) {
        if (this.hasLink) {
            this.linkOffset = offsetAtEndOfPart - this.linkLength - 2;
        }
        this.offsetAtStartOfPart = offsetAtEndOfPart - this.getVisibleTextLength();
    }

    public void toggleFolded() {
        this.folded = !this.folded;
    }

    public List<? extends StyleRange> getStyleRanges() {
        final List<StyleRange> ret = new ArrayList<>();
        if (this.folded) {
            ret.addAll(this.textFolded.getStyles(this.offsetAtStartOfPart));
        } else {
            ret.addAll(this.text.getStyles(this.offsetAtStartOfPart));
        }
        if (this.hasLink) {
            final Display display = Display.getCurrent();
            final Color color = display.getSystemColor(SWT.COLOR_BLUE);

            final StyleRange styleRange = new StyleRange(this.getOffset(), this.getLength(), color, null);
            styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            styleRange.underline = true;
            ret.add(styleRange);
        }
        return ret;
    }

}