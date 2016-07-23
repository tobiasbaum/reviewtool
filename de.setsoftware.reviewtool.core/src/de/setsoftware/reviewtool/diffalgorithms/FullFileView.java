package de.setsoftware.reviewtool.diffalgorithms;

/**
 * Basic {@link OneFileView} with the whole contents of a file.
 * @param <T> Type of the items in the sequence.
 */
final class FullFileView<T> extends OneFileView<T> {
    private final T[] fullFile;

    public FullFileView(T[] lines) {
        this.fullFile = lines;
    }

    @Override
    public int getLineCount() {
        return this.fullFile.length;
    }

    @Override
    public T getLine(int i) {
        return this.fullFile[i];
    }

    @Override
    public int toIndexInWholeFile(int index) {
        return index;
    }

    @Override
    public OneFileView<T> subrange(int start, int end) {
        return new RangeView<>(this, start, end);
    }

}