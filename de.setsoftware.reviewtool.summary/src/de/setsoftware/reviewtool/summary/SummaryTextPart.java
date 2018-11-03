package de.setsoftware.reviewtool.summary;

import org.eclipse.jface.text.IRegion;

/**
 * Single part of a summary text that can be folded.
 */
class SummaryTextPart implements IRegion {
    private String text = "";
    private String textFolded = "";
    private int lines = 0;
    private int linesFolded = 0;
    private boolean folded = false;
    private boolean hasLink = false; // No hyper-link, if content is short enough

    private int linkOffset = 0; // Character offset of whole summary text, used for displaying links
    private int linkLength = 0; // Links with length 0 are not presented

    public SummaryTextPart() {
    }

    public SummaryTextPart(String refDiff) {
        this.text = refDiff;
        this.lines = refDiff.split("\n").length;
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
        this.text += string + "\n";
        this.lines++;
    }

    public void addLinkIfNeeded(int maxLinesFolded) {
        if (this.lines > maxLinesFolded) {
            final String[] text = this.text.split("\n");
            final int max = Math.max(2, maxLinesFolded);
            for (int i = 0; i < max - 1; i++) {
                this.textFolded = this.textFolded + text[i] + "\n";
                this.linesFolded++;
            }
            this.hasLink = true;
            this.folded = true;
            this.linkLength = 7; // Link length (more.. or less...)
            this.text = this.text + "less..." + "\n\n";
            this.textFolded = this.textFolded + "(" + (this.lines - this.linesFolded) + " more lines) show...\n\n";
        } else {
            this.text = this.text + "\n";
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
            return this.textFolded;
        } else {
            return this.text;
        }
    }

    public int getVisibleLineCount() {
        if (this.folded) {
            return this.linesFolded;
        } else {
            return this.lines;
        }
    }

    public void updateLinkOffset(int offset) {
        if (this.hasLink) {
            this.linkOffset = offset - this.linkLength - 2;
        }
    }

    public void toggleFolded() {
        this.folded = !this.folded;
    }

}