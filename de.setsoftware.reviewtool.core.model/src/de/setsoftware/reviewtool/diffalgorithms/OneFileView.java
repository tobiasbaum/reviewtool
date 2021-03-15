package de.setsoftware.reviewtool.diffalgorithms;

/**
 * Helper class that provided a view into a subset of the items in a sequence (e.g. lines in a file).
 * @param <T> Type of the items in the sequence.
 */
abstract class OneFileView<T> {

    public abstract T getItem(int i);

    public final T getItemFromEnd(int i) {
        return this.getItem(this.getItemCount() - 1 - i);
    }

    public abstract int getItemCount();

    public abstract int toIndexInWholeFile(int index);

    public final OneFileView<T> stripPrefix(int prefixLength) {
        return this.subrange(prefixLength, this.getItemCount());
    }

    public final OneFileView<T> stripSuffix(int suffixLength) {
        return this.subrange(0, this.getItemCount() - suffixLength);
    }

    /**
     * Creates a subrange of the current sequence.
     * @param start The start index (inclusive).
     * @param end The end index (exclusive).
     */
    public abstract OneFileView<T> subrange(int start, int end);

    @Override
    public String toString() {
        return this.getContent();
    }

    public String getContent() {
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < this.getItemCount(); i++) {
            ret.append(this.getItem(i)).append('\n');
        }
        return ret.toString();
    }

}