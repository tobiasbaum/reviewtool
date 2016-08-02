package de.setsoftware.reviewtool.model.changestructure;

/**
 * A singular change of a part of a text file.
 */
public class TextualChangeHunk extends Change {

    private final Fragment from;
    private final Fragment to;

    TextualChangeHunk(Fragment from, Fragment to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void accept(ChangeVisitor visitor) {
        visitor.handle(this);
    }

    public Fragment getFrom() {
        return this.from;
    }

    public Fragment getTo() {
        return this.to;
    }

}
