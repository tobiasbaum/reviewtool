package de.setsoftware.reviewtool.base;

/**
 * A simple value wrapper, mostly for use with visitor and other anonymous classes.
 * @param <T> Type of the wrapped variable.
 */
public class ValueWrapper<T> {

    private T value;

    public ValueWrapper() {
        this(null);
    }

    public ValueWrapper(T value) {
        this.value = value;
    }

    public T get() {
        return this.value;
    }

    public void setValue(T v) {
        this.value = v;
    }

}
