package de.setsoftware.reviewtool.model.changestructure;

/**
 * A singular change of a part of a text file.
 */
public class TextualChange extends Change {

    private final FileFragment from;
    private final FileFragment to;

    public TextualChange(FileFragment from, FileFragment to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void accept(ChangeVisitor visitor) {
        visitor.handle(this);
    }

    public FileFragment getFrom() {
        return this.from;
    }

    public FileFragment getTo() {
        return this.to;
    }

}
