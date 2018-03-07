package de.setsoftware.reviewtool.summary;

import org.eclipse.jface.text.IRegion;

/**
 * Single part of a summary text that can be folded.
 */
class SummaryTextPart implements IRegion {
    String text = "";
    String textFolded = "";
    int lines = 0;
    int linesFolded = 0;
    int maxLinesFolded = 4;
    boolean folded = false;
    boolean hasLink = false; // No hyper-link, if content is short enough

    int linkOffset = 0; // Character offset of whole summary text, used for displaying links
    int linkLength = 0; // Links with length 0 are not presented

    @Override
    public int getLength() {
        return linkLength;
    }

    @Override
    public int getOffset() {
        return linkOffset;
    }
}