package de.setsoftware.reviewtool.diffalgorithms;

/**
 * Continuous subsequence of the lines in a file.
 * @param <T> Type of the items in the sequence.
 */
final class RangeView<T> extends OneFileView<T> {
    private final OneFileView<T> decorated;
    private final int start;
    private final int end;

    RangeView(OneFileView<T> decorated, int start, int end) {
        this.decorated = decorated;
        this.start = start;
        this.end = end;
    }

    @Override
    public int getLineCount() {
        return this.end - this.start;
    }

    @Override
    public T getLine(int i) {
        return this.decorated.getLine(this.start + i);
    }

    @Override
    public int toIndexInWholeFile(int index) {
        return this.decorated.toIndexInWholeFile(index + this.start);
    }

    @Override
    public OneFileView<T> subrange(int start, int end) {
        return new RangeView<>(this.decorated, this.start + start, this.start + end);
    }

}